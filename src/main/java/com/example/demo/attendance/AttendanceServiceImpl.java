package com.example.demo.attendance;

import com.example.demo.config.HrmsProperties;
import com.example.demo.exception.CustomException;
import com.example.demo.overtime.OvertimeService;
import com.example.demo.site.Site;
import com.example.demo.site.SiteRepository;
import com.example.demo.worker.Worker;
import com.example.demo.worker.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final WorkerRepository workerRepository;
    private final SiteRepository siteRepository;
    private final OvertimeService overtimeService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final HrmsProperties globalProperties;

    private static final String ACTIVE_WORKERS_KEY_PREFIX = "active_worker:";

    @Override
    @Transactional
    public void clockIn(ClockInRequest request) {
        Worker worker = workerRepository.findByIdAndActiveStatusTrue(request.getWorkerId())
                .orElseThrow(() -> new CustomException("WORKER_NOT_FOUND", "Worker not found or inactive", HttpStatus.NOT_FOUND));

        Site site = siteRepository.findByIdAndActiveStatusTrue(request.getSiteId())
                .orElseThrow(() -> new CustomException("SITE_NOT_FOUND", "Site not found or inactive", HttpStatus.NOT_FOUND));

        if (attendanceRepository.findByWorkerIdAndClockOutTimeIsNull(worker.getId()).isPresent()) {
            throw new CustomException("DUPLICATE_CLOCK_IN", "Worker is already clocked in", HttpStatus.CONFLICT);
        }

        AttendanceLog attendanceLog = AttendanceLog.builder()
                .worker(worker)
                .site(site)
                .clockInTime(LocalDateTime.now())
                .flagged(false)
                .build();

        attendanceRepository.save(attendanceLog);

        ActiveWorkerDTO cacheDto = ActiveWorkerDTO.builder()
                .workerId(worker.getId())
                .workerName(worker.getName())
                .siteId(site.getId())
                .siteName(site.getSiteName())
                .clockInTime(attendanceLog.getClockInTime())
                .build();

        long maxShiftHours = site.getCustomMaxShiftHours() != null ? 
                site.getCustomMaxShiftHours() : globalProperties.getMaxShiftHours();

        try {
            redisTemplate.opsForValue().set(ACTIVE_WORKERS_KEY_PREFIX + worker.getId(), cacheDto, Duration.ofHours(maxShiftHours));
        } catch (Exception e) {
            log.error("Failed to cache active worker in Redis: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void clockOut(ClockOutRequest request) {
        AttendanceLog logEntry = attendanceRepository.findByWorkerIdAndClockOutTimeIsNull(request.getWorkerId())
                .orElseThrow(() -> new CustomException("CLOCK_OUT_ERROR", "Worker is not currently clocked in", HttpStatus.BAD_REQUEST));

        LocalDateTime clockOutTime = LocalDateTime.now();
        logEntry.setClockOutTime(clockOutTime);

        long minutes = ChronoUnit.MINUTES.between(logEntry.getClockInTime(), clockOutTime);
        double hours = minutes / 60.0;
        logEntry.setTotalHoursWorked(hours);

        long maxShiftHours = logEntry.getSite().getCustomMaxShiftHours() != null ? 
                logEntry.getSite().getCustomMaxShiftHours() : globalProperties.getMaxShiftHours();

        if (hours > maxShiftHours) {
            logEntry.setFlagged(true);
        }

        overtimeService.calculateAndSaveOvertime(logEntry);
        attendanceRepository.save(logEntry);

        try {
            redisTemplate.delete(ACTIVE_WORKERS_KEY_PREFIX + request.getWorkerId());
        } catch (Exception e) {
            log.error("Failed to remove worker from Redis cache: {}", e.getMessage());
        }
    }
@Override
public List<ActiveWorkerDTO> getActiveWorkers() {
    try {
        Set<String> keys = redisTemplate.keys(ACTIVE_WORKERS_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            return keys.stream()
                    .map(key -> (ActiveWorkerDTO) redisTemplate.opsForValue().get(key))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    } catch (Exception e) {
        log.error("Redis error, falling back to database: {}", e.getMessage());
    }

    // Self-healing: Cache is empty or Redis is down, fetch from DB
    List<ActiveWorkerDTO> activeFromDb = attendanceRepository.findAllByClockOutTimeIsNull().stream()
            .map(log -> ActiveWorkerDTO.builder()
                    .workerId(log.getWorker().getId())
                    .workerName(log.getWorker().getName())
                    .siteId(log.getSite().getId())
                    .siteName(log.getSite().getSiteName())
                    .clockInTime(log.getClockInTime())
                    .build())
            .collect(Collectors.toList());

    // Re-populate Redis in background
    if (!activeFromDb.isEmpty()) {
        activeFromDb.forEach(dto -> {
            try {
                // We need the site to get the custom max shift hours
                Site site = siteRepository.findById(dto.getSiteId()).orElse(null);
                long maxShiftHours = (site != null && site.getCustomMaxShiftHours() != null) ? 
                        site.getCustomMaxShiftHours() : globalProperties.getMaxShiftHours();
                        
                redisTemplate.opsForValue().set(ACTIVE_WORKERS_KEY_PREFIX + dto.getWorkerId(), dto, Duration.ofHours(maxShiftHours));
            } catch (Exception ignored) {}
        });
    }

    return activeFromDb;
}

    @Override
    public Page<AttendanceLog> getAttendanceLog(Long workerId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return attendanceRepository.findByWorkerIdAndClockInTimeBetween(workerId, from, to, pageable);
    }

    @Override
    public WorkerHistoryResponse getWorkerHistory(Long workerId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new CustomException("WORKER_NOT_FOUND", "Worker not found", HttpStatus.NOT_FOUND));

        Page<AttendanceLog> logPage = attendanceRepository.findByWorkerIdAndClockInTimeBetween(workerId, from, to, pageable);
        
        List<AttendanceLogDTO> dtoList = logPage.getContent().stream()
                .map(log -> AttendanceLogDTO.builder()
                        .id(log.getId())
                        .siteId(log.getSite().getId())
                        .siteName(log.getSite().getSiteName())
                        .clockInTime(log.getClockInTime())
                        .clockOutTime(log.getClockOutTime())
                        .totalHoursWorked(log.getTotalHoursWorked())
                        .overtimeHours(log.getOvertimeHours())
                        .flagged(log.getFlagged())
                        .build())
                .collect(Collectors.toList());

        Page<AttendanceLogDTO> dtoPage = new org.springframework.data.domain.PageImpl<>(dtoList, pageable, logPage.getTotalElements());

        return WorkerHistoryResponse.builder()
                .workerId(worker.getId())
                .workerName(worker.getName())
                .attendanceLogs(com.example.demo.common.PaginatedResponse.fromPage(dtoPage))
                .build();
    }

    @Override
    public void invalidateWorkerCache(Long workerId) {
        try {
            redisTemplate.delete(ACTIVE_WORKERS_KEY_PREFIX + workerId);
        } catch (Exception e) {
            log.error("Failed to invalidate cache for worker {}: {}", workerId, e.getMessage());
        }
    }
}
