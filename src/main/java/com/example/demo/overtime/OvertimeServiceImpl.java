package com.example.demo.overtime;

import com.example.demo.config.HrmsProperties;
import com.example.demo.attendance.AttendanceLog;
import com.example.demo.attendance.AttendanceRepository;
import com.example.demo.common.SettlementStatus;
import com.example.demo.exception.CustomException;
import com.example.demo.site.Site;
import com.example.demo.worker.Worker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OvertimeServiceImpl implements OvertimeService {

    private final OvertimeRepository overtimeRepository;
    private final AttendanceRepository attendanceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final HrmsProperties globalProperties;

    @Override
    @Transactional
    public void calculateAndSaveOvertime(AttendanceLog log) {
        Site site = log.getSite();
        
        // Use Site Overrides or Global Fallbacks
        double standardShiftHours = site.getCustomStandardShiftHours() != null ? 
                site.getCustomStandardShiftHours() : globalProperties.getStandardShiftHours();
        
        double monthlyOvertimeCap = site.getCustomMonthlyOvertimeCap() != null ? 
                site.getCustomMonthlyOvertimeCap() : globalProperties.getMonthlyOvertimeCap();
        
        double otRate1_5Limit = globalProperties.getOtRate1_5Limit();

        double totalHours = log.getTotalHoursWorked();
        if (totalHours <= standardShiftHours) {
            log.setOvertimeHours(0.0);
            return;
        }

        double overtimeHours = totalHours - standardShiftHours;
        
        LocalDateTime startOfMonth = log.getClockInTime().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = log.getClockInTime().with(YearMonth.from(log.getClockInTime()).atEndOfMonth().atTime(23, 59, 59));
        
        Double currentMonthOvertime = attendanceRepository.getTotalOvertimeHoursForMonth(log.getWorker().getId(), startOfMonth, endOfMonth);
        if (currentMonthOvertime == null) currentMonthOvertime = 0.0;
        
        double remainingCap = monthlyOvertimeCap - currentMonthOvertime;
        if (remainingCap <= 0) {
            log.setOvertimeHours(0.0);
            return;
        }
        
        if (overtimeHours > remainingCap) {
            overtimeHours = remainingCap;
        }
        
        log.setOvertimeHours(overtimeHours);
        
        Worker worker = log.getWorker();
        BigDecimal hourlyRate = worker.getDailyWageRate().divide(BigDecimal.valueOf(standardShiftHours), 2, RoundingMode.HALF_UP);
        
        BigDecimal amount = BigDecimal.ZERO;
        double firstTierOT = Math.min(overtimeHours, otRate1_5Limit);
        double secondTierOT = Math.max(0, overtimeHours - otRate1_5Limit);
        
        BigDecimal rate1_5 = hourlyRate.multiply(BigDecimal.valueOf(1.5));
        BigDecimal rate2_0 = hourlyRate.multiply(BigDecimal.valueOf(2.0));
        
        amount = amount.add(rate1_5.multiply(BigDecimal.valueOf(firstTierOT)));
        amount = amount.add(rate2_0.multiply(BigDecimal.valueOf(secondTierOT)));
        
        OvertimeEntry entry = OvertimeEntry.builder()
                .worker(worker)
                .attendance(log)
                .date(log.getClockInTime().toLocalDate())
                .overtimeHours(overtimeHours)
                .overtimeRateApplied(rate1_5)
                .amount(amount.setScale(2, RoundingMode.HALF_UP))
                .settlementStatus(SettlementStatus.PENDING)
                .build();
        
        overtimeRepository.save(entry);
    }

    @Override
    public List<OvertimeEntry> getOvertimeSummary(Long workerId, String month) {
        YearMonth ym = YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return overtimeRepository.findByWorkerIdAndDateBetween(workerId, start, end);
    }

    @Override
    @Transactional
    public BigDecimal settleOvertime(Long workerId, String month) {
        YearMonth ym = YearMonth.parse(month);
        if (ym.equals(YearMonth.now())) {
            throw new CustomException("SETTLEMENT_ERROR", "Cannot settle current month", HttpStatus.BAD_REQUEST);
        }

        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<OvertimeEntry> entries = overtimeRepository.findByWorkerIdAndDateBetweenAndSettlementStatus(
                workerId, start, end, SettlementStatus.PENDING);

        if (entries.isEmpty()) {
            List<OvertimeEntry> allEntries = overtimeRepository.findByWorkerIdAndDateBetween(workerId, start, end);
            if (allEntries.isEmpty()) {
                throw new CustomException("SETTLEMENT_ERROR", "No overtime entries found for this month", HttpStatus.NOT_FOUND);
            }
            throw new CustomException("SETTLEMENT_ALREADY_DONE", "All entries for this month are already settled", HttpStatus.CONFLICT);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OvertimeEntry entry : entries) {
            entry.setSettlementStatus(SettlementStatus.SETTLED);
            totalAmount = totalAmount.add(entry.getAmount());
        }
        
        overtimeRepository.saveAll(entries);
        eventPublisher.publishEvent(new OvertimeSettledEvent(this, workerId, month, totalAmount));
        
        return totalAmount;
    }
}
