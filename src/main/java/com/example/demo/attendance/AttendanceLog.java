package com.example.demo.attendance;

import com.example.demo.common.BaseEntity;
import com.example.demo.site.Site;
import com.example.demo.worker.Worker;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_log", indexes = {
    @Index(name = "idx_attendance_worker_id", columnList = "worker_id"),
    @Index(name = "idx_attendance_site_id", columnList = "site_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime clockInTime;

    private LocalDateTime clockOutTime;

    private Double totalHoursWorked;

    private Double overtimeHours;

    @Column(nullable = false)
    @Builder.Default
    private Boolean flagged = false;
}
