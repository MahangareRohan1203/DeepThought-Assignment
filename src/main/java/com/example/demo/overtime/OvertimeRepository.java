package com.example.demo.overtime;

import com.example.demo.common.SettlementStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OvertimeRepository extends JpaRepository<OvertimeEntry, Long> {
    @EntityGraph(attributePaths = {"worker", "attendance", "attendance.site"})
    List<OvertimeEntry> findByWorkerIdAndDateBetween(Long workerId, LocalDate start, LocalDate end);
    
    @EntityGraph(attributePaths = {"worker", "attendance"})
    List<OvertimeEntry> findByWorkerIdAndDateBetweenAndSettlementStatus(Long workerId, LocalDate start, LocalDate end, SettlementStatus status);
}
