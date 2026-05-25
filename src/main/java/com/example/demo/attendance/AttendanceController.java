package com.example.demo.attendance;

import com.example.demo.common.PaginatedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/clock-in")
    public ResponseEntity<String> clockIn(@Valid @RequestBody ClockInRequest request) {
        attendanceService.clockIn(request);
        return ResponseEntity.ok("Clock-in successful");
    }

    @PostMapping("/clock-out")
    public ResponseEntity<String> clockOut(@Valid @RequestBody ClockOutRequest request) {
        attendanceService.clockOut(request);
        return ResponseEntity.ok("Clock-out successful");
    }

    @GetMapping("/active")
    public ResponseEntity<List<ActiveWorkerDTO>> getActiveWorkers() {
        return ResponseEntity.ok(attendanceService.getActiveWorkers());
    }

    @GetMapping("/log")
    public ResponseEntity<WorkerHistoryResponse> getAttendanceLog(
            @RequestParam Long workerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(attendanceService.getWorkerHistory(workerId, from, to, pageable));
    }
}
