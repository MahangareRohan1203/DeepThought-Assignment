package com.example.demo.attendance;

import com.example.demo.config.HrmsProperties;
import com.example.demo.exception.CustomException;
import com.example.demo.overtime.OvertimeService;
import com.example.demo.site.Site;
import com.example.demo.site.SiteRepository;
import com.example.demo.worker.Worker;
import com.example.demo.worker.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private WorkerRepository workerRepository;
    @Mock
    private SiteRepository siteRepository;
    @Mock
    private OvertimeService overtimeService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private HrmsProperties globalProperties;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private Worker testWorker;
    private Site testSite;

    @BeforeEach
    void setUp() {
        testWorker = Worker.builder().id(1L).name("John").activeStatus(true).build();
        testSite = Site.builder().id(1L).siteName("Site A").activeStatus(true).build();
    }

    @Test
    void clockIn_Success() {
        ClockInRequest request = new ClockInRequest();
        request.setWorkerId(1L);
        request.setSiteId(1L);

        when(workerRepository.findByIdAndActiveStatusTrue(1L)).thenReturn(Optional.of(testWorker));
        when(siteRepository.findByIdAndActiveStatusTrue(1L)).thenReturn(Optional.of(testSite));
        when(attendanceRepository.findByWorkerIdAndClockOutTimeIsNull(1L)).thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(globalProperties.getMaxShiftHours()).thenReturn(16L);

        assertDoesNotThrow(() -> attendanceService.clockIn(request));

        verify(attendanceRepository).save(any(AttendanceLog.class));
        verify(valueOperations).set(anyString(), any(), any());
    }

    @Test
    void clockIn_Duplicate_ThrowsException() {
        ClockInRequest request = new ClockInRequest();
        request.setWorkerId(1L);
        request.setSiteId(1L);

        when(workerRepository.findByIdAndActiveStatusTrue(1L)).thenReturn(Optional.of(testWorker));
        when(siteRepository.findByIdAndActiveStatusTrue(1L)).thenReturn(Optional.of(testSite));
        when(attendanceRepository.findByWorkerIdAndClockOutTimeIsNull(1L)).thenReturn(Optional.of(new AttendanceLog()));

        CustomException ex = assertThrows(CustomException.class, () -> attendanceService.clockIn(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("DUPLICATE_CLOCK_IN", ex.getError());
    }

    @Test
    void clockOut_Success_FlagsLongShift() {
        ClockOutRequest request = new ClockOutRequest();
        request.setWorkerId(1L);

        AttendanceLog log = AttendanceLog.builder()
                .worker(testWorker)
                .site(testSite)
                .clockInTime(LocalDateTime.now().minusHours(17)) // Longer than 16h
                .build();

        when(attendanceRepository.findByWorkerIdAndClockOutTimeIsNull(1L)).thenReturn(Optional.of(log));
        when(globalProperties.getMaxShiftHours()).thenReturn(16L);

        attendanceService.clockOut(request);

        assertTrue(log.getFlagged());
        assertNotNull(log.getClockOutTime());
        verify(overtimeService).calculateAndSaveOvertime(log);
        verify(attendanceRepository).save(log);
        verify(redisTemplate).delete(anyString());
    }

    @Test
    void getActiveWorkers_FallbackToDb() {
        // Redis returns empty
        when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());
        
        AttendanceLog log = AttendanceLog.builder()
                .worker(testWorker)
                .site(testSite)
                .clockInTime(LocalDateTime.now())
                .build();
        
        when(attendanceRepository.findAllByClockOutTimeIsNull()).thenReturn(List.of(log));
        when(globalProperties.getMaxShiftHours()).thenReturn(16L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        List<ActiveWorkerDTO> result = attendanceService.getActiveWorkers();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getWorkerName());
        
        // Verify self-healing
        verify(valueOperations).set(anyString(), any(), any());
    }
}
