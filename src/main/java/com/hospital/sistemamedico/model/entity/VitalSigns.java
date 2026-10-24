package com.hospital.sistemamedico.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Signos vitales tomados por enfermería (CU-07). Un registro por cita. */
@Entity
@Table(name = "vital_signs")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"appointment", "recordedBy"})
public class VitalSigns {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by", nullable = false)
    private InternalUser recordedBy;

    @Column(nullable = false)
    private Integer systolic;

    @Column(nullable = false)
    private Integer diastolic;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal temperature;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal height;

    @Column(name = "heart_rate", nullable = false)
    private Integer heartRate;

    @Column(nullable = false)
    private boolean emergency = false;

    /** Alertas clínicas detectadas (RN-CU07-06), separadas por " | ". Null si todo es normal. */
    @Column(length = 500)
    private String alerts;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();
}
