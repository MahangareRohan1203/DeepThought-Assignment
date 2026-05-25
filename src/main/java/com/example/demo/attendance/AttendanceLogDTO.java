package com.example.demo.attendance;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AttendanceLogDTO {
    private Long id;
    private Long siteId;
    private String siteName;
    private LocalDateTime clockInTime;
    private LocalDateTime clockOutTime;
    private Double totalHoursWorked;
    private Double overtimeHours;
    private Boolean flagged;
}
