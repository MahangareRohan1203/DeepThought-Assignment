package com.example.demo.worker;

import com.example.demo.common.BaseEntity;
import com.example.demo.common.Designation;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "worker")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Worker extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String phone;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Designation designation;

    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyWageRate;

    @NotNull
    @Column(nullable = false)
    @Builder.Default
    private Boolean activeStatus = true;
}
