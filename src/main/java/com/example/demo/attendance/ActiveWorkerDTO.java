package com.example.demo.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActiveWorkerDTO {
    private Long workerId;
    private String workerName;
    private Long siteId;
    private String siteName;
    private LocalDateTime clockInTime;
}
