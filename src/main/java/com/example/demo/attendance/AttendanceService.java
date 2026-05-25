package com.example.demo.attendance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AttendanceService {
    void clockIn(ClockInRequest request);
    void clockOut(ClockOutRequest request);
    List<ActiveWorkerDTO> getActiveWorkers();
    Page<AttendanceLog> getAttendanceLog(Long workerId, LocalDateTime from, LocalDateTime to, Pageable pageable);
    WorkerHistoryResponse getWorkerHistory(Long workerId, LocalDateTime from, LocalDateTime to, Pageable pageable);
    void invalidateWorkerCache(Long workerId);
}
