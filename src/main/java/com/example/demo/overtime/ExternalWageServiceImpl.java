package com.example.demo.overtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class ExternalWageServiceImpl implements ExternalWageService {

    @Override
    public BigDecimal getMinimumWageRate() {
        log.info("Fetching minimum wage rate from external API...");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            log.error("External API call interrupted");
        }
        return new BigDecimal("500.00");
    }
}
