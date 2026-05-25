package com.example.demo.attendance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceLog, Long> {

    @EntityGraph(attributePaths = {"worker", "site"})
    Optional<AttendanceLog> findByWorkerIdAndClockOutTimeIsNull(Long workerId);

    @EntityGraph(attributePaths = {"worker", "site"})
    Page<AttendanceLog> findByWorkerIdAndClockInTimeBetween(Long workerId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    @EntityGraph(attributePaths = {"worker", "site"})
    List<AttendanceLog> findAllByClockOutTimeIsNull();

    @Query("SELECT SUM(a.overtimeHours) FROM AttendanceLog a WHERE a.worker.id = :workerId AND a.clockInTime >= :start AND a.clockInTime <= :end")
    Double getTotalOvertimeHoursForMonth(Long workerId, LocalDateTime start, LocalDateTime end);
}
