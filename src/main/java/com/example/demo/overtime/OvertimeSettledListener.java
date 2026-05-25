package com.example.demo.overtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class OvertimeSettledListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOvertimeSettled(OvertimeSettledEvent event) {
        log.info("Sending SMS notification for worker {}: Your {} overtime of {} has been settled.",
                event.getWorkerId(), event.getMonth(), event.getAmount());
        
        // Simulate external SMS API call
        try {
            Thread.sleep(1000);
            log.info("SMS sent successfully to worker {}", event.getWorkerId());
        } catch (InterruptedException e) {
            log.error("Failed to send SMS for worker {}", event.getWorkerId());
        }
    }
}
