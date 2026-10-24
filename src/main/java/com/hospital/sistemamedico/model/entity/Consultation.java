package com.hospital.sistemamedico.model.entity;

import com.hospital.sistemamedico.model.enums.ConsultationStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/** Consulta médica (CU-08). Una por cita. */
@Entity
@Table(name = "consultation")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"appointment"})
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(name = "visit_reason", nullable = false, length = 2000)
    private String visitReason;

    @Column(name = "clinical_findings", length = 5000)
    private String clinicalFindings;

    @Column(name = "cie10_code", length = 10)
    private String cie10Code;

    @Column(length = 5000)
    private String diagnosis;

    @Column(name = "treatment_plan", length = 5000)
    private String treatmentPlan;

    @Column(length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsultationStatus status = ConsultationStatus.EN_CURSO;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Version
    private Long version;
}
