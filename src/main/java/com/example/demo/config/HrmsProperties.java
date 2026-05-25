package com.example.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "hrms.business-rules")
public class HrmsProperties {
    private Double standardShiftHours = 8.0;
    private Double monthlyOvertimeCap = 60.0;
    private Double otRate1_5Limit = 2.0;
    private Long maxShiftHours = 16L;
}
