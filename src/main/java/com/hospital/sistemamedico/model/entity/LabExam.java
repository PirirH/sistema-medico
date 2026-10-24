package com.hospital.sistemamedico.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "lab_exam")
@Data
@NoArgsConstructor
public class LabExam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "default_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal defaultAmount;

    @Column(name = "reference_range", length = 100)
    private String referenceRange;

    @Column(length = 50)
    private String unit;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "laboratory_id", nullable = false)
    private Laboratory laboratory;

    @Column(nullable = false)
    private boolean active = true;
}
