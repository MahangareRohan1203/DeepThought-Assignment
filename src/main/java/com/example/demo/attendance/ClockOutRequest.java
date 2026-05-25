package com.example.demo.attendance;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClockOutRequest {
    @NotNull
    private Long workerId;
}
