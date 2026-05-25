package com.example.demo.overtime;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/overtime")
@RequiredArgsConstructor
public class OvertimeController {

    private final OvertimeService overtimeService;
    private final ExternalWageService externalWageService;

    @GetMapping("/summary/{workerId}")
    public ResponseEntity<OvertimeSummaryResponse> getOvertimeSummary(
            @PathVariable Long workerId,
            @RequestParam String month) {
        
        // Ticket LF-205: Fetch external data BEFORE transaction (if any)
        BigDecimal minWage = externalWageService.getMinimumWageRate();
        
        List<OvertimeEntry> entries = overtimeService.getOvertimeSummary(workerId, month);
        BigDecimal totalAmount = entries.stream()
                .map(OvertimeEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return ResponseEntity.ok(new OvertimeSummaryResponse(entries, totalAmount, minWage));
    }

    @PostMapping("/settle/{workerId}")
    public ResponseEntity<SettlementResponse> settleOvertime(
            @PathVariable Long workerId,
            @RequestParam String month) {
        BigDecimal totalAmount = overtimeService.settleOvertime(workerId, month);
        return ResponseEntity.ok(new SettlementResponse(totalAmount, "Settlement successful for " + month));
    }

    private record SettlementResponse(BigDecimal totalAmount, String message) {}
    private record OvertimeSummaryResponse(List<OvertimeEntry> entries, BigDecimal totalPayout, BigDecimal currentMinWage) {}
}
