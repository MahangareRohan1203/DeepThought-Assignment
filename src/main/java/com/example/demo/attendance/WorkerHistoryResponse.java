package com.example.demo.attendance;

import com.example.demo.common.PaginatedResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkerHistoryResponse {
    private Long workerId;
    private String workerName;
    private PaginatedResponse<AttendanceLogDTO> attendanceLogs;
}
