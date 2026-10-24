package com.hospital.sistemamedico.model.entity;

import com.hospital.sistemamedico.model.enums.AppointmentChannel;
import com.hospital.sistemamedico.model.enums.AppointmentPriority;
import com.hospital.sistemamedico.model.enums.AppointmentStatus;
import com.hospital.sistemamedico.model.enums.FollowUpType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointment")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"patient", "doctor", "branchSpecialty"})
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "appointment_number", nullable = false, unique = true, length = 20)
    private String appointmentNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private InternalUser doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_specialty_id", nullable = false)
    private BranchSpecialty branchSpecialty;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private AppointmentChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private AppointmentPriority priority = AppointmentPriority.NORMAL;

    @Column(name = "reason", nullable = false, length = 2000)
    private String reason;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "reservation_expires_at")
    private LocalDateTime reservationExpiresAt;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    // ---- Fase 3 (CU-12): citas de seguimiento ----
    @Enumerated(EnumType.STRING)
    @Column(name = "follow_up_type", length = 30)
    private FollowUpType followUpType;

    /** Id de la consulta que originó el seguimiento (null si no es una cita de seguimiento). */
    @Column(name = "parent_consultation_id")
    private Integer parentConsultationId;

    /** RN-CU11-05: true cuando ya se envió el recordatorio automático. */
    @Column(name = "reminder_sent", nullable = false)
    private boolean reminderSent = false;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
