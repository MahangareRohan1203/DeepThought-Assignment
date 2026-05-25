package com.example.demo.site;

import com.example.demo.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "site")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Site extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String siteName;

    @NotBlank
    @Column(nullable = false)
    private String location;

    @NotNull
    @Column(nullable = false)
    @Builder.Default
    private Boolean activeStatus = true;

    // Business Rule Overrides (Optional)
    private Double customStandardShiftHours;
    private Double customMonthlyOvertimeCap;
    private Long customMaxShiftHours;
}
