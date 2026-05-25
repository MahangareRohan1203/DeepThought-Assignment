package com.example.demo.attendance;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClockInRequest {
    @NotNull
    private Long workerId;
    @NotNull
    private Long siteId;
}
