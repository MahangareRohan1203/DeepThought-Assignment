package com.example.demo.overtime;

import com.example.demo.attendance.AttendanceLog;
import com.example.demo.attendance.AttendanceRepository;
import com.example.demo.common.Designation;
import com.example.demo.common.SettlementStatus;
import com.example.demo.config.HrmsProperties;
import com.example.demo.site.Site;
import com.example.demo.worker.Worker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OvertimeServiceTest {

    @Mock
    private OvertimeRepository overtimeRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private HrmsProperties globalProperties;

    @InjectMocks
    private OvertimeServiceImpl overtimeService;

    private Worker worker;
    private Site site;

    @BeforeEach
    void setUp() {
        worker = Worker.builder()
                .id(1L)
                .name("Test Worker")
                .dailyWageRate(new BigDecimal("800.00")) // 100 per hour (based on 8h)
                .designation(Designation.MASON)
                .activeStatus(true)
                .build();
        
        site = Site.builder().id(1L).siteName("Global Site").build();
    }

    @Test
    void calculateAndSaveOvertime_UseGlobalDefaults() {
        // Global defaults: 8h shift, 60h cap, 2h 1.5x limit
        when(globalProperties.getStandardShiftHours()).thenReturn(8.0);
        when(globalProperties.getMonthlyOvertimeCap()).thenReturn(60.0);
        when(globalProperties.getOtRate1_5Limit()).thenReturn(2.0);
        
        // 11 hours total = 8 standard + 2 OT @ 1.5x + 1 OT @ 2.0x
        AttendanceLog log = AttendanceLog.builder()
                .worker(worker)
                .site(site)
                .clockInTime(LocalDateTime.of(2026, 5, 25, 8, 0))
                .totalHoursWorked(11.0)
                .build();

        when(attendanceRepository.getTotalOvertimeHoursForMonth(any(), any(), any())).thenReturn(0.0);

        overtimeService.calculateAndSaveOvertime(log);

        assertEquals(3.0, log.getOvertimeHours());
        
        ArgumentCaptor<OvertimeEntry> entryCaptor = ArgumentCaptor.forClass(OvertimeEntry.class);
        verify(overtimeRepository).save(entryCaptor.capture());
        
        OvertimeEntry entry = entryCaptor.getValue();
        // 2 hours * 100 * 1.5 = 300
        // 1 hour * 100 * 2.0 = 200
        // Total = 500
        assertEquals(new BigDecimal("500.00"), entry.getAmount());
    }

    @Test
    void calculateAndSaveOvertime_UseSiteOverrides() {
        // Site override: 10h shift
        site.setCustomStandardShiftHours(10.0);
        when(globalProperties.getMonthlyOvertimeCap()).thenReturn(60.0);
        when(globalProperties.getOtRate1_5Limit()).thenReturn(2.0);
        
        // 13 hours total = 10 standard + 2 OT @ 1.5x + 1 OT @ 2.0x
        AttendanceLog log = AttendanceLog.builder()
                .worker(worker)
                .site(site)
                .clockInTime(LocalDateTime.of(2026, 5, 25, 8, 0))
                .totalHoursWorked(13.0)
                .build();

        when(attendanceRepository.getTotalOvertimeHoursForMonth(any(), any(), any())).thenReturn(0.0);

        overtimeService.calculateAndSaveOvertime(log);

        assertEquals(3.0, log.getOvertimeHours());
        
        ArgumentCaptor<OvertimeEntry> entryCaptor = ArgumentCaptor.forClass(OvertimeEntry.class);
        verify(overtimeRepository).save(entryCaptor.capture());
        
        OvertimeEntry entry = entryCaptor.getValue();
        // Hourly rate = 800 / 10 = 80
        // 2 hours * 80 * 1.5 = 240
        // 1 hour * 80 * 2.0 = 160
        // Total = 400
        assertEquals(new BigDecimal("400.00"), entry.getAmount());
    }

    @Test
    void settleOvertime_Success_PublishesEvent() {
        OvertimeEntry entry = OvertimeEntry.builder().id(1L).amount(new BigDecimal("100")).settlementStatus(SettlementStatus.PENDING).build();
        when(overtimeRepository.findByWorkerIdAndDateBetweenAndSettlementStatus(any(), any(), any(), eq(SettlementStatus.PENDING)))
                .thenReturn(List.of(entry));

        BigDecimal total = overtimeService.settleOvertime(1L, "2026-04");

        assertEquals(new BigDecimal("100"), total);
        assertEquals(SettlementStatus.SETTLED, entry.getSettlementStatus());
        verify(eventPublisher).publishEvent(any(OvertimeSettledEvent.class));
    }
}
