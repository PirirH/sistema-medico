package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    // ---- Consultas con JOIN FETCH: traen las relaciones ya cargadas, porque con
    // ---- open-in-view=false la sesión de Hibernate se cierra antes de renderizar la vista.

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.patient
            JOIN FETCH a.doctor
            JOIN FETCH a.branchSpecialty bs
            JOIN FETCH bs.branch
            JOIN FETCH bs.specialty
            WHERE a.id = :id
            """)
    Optional<Appointment> findByIdWithDetails(@Param("id") Integer id);

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.patient
            JOIN FETCH a.doctor
            JOIN FETCH a.branchSpecialty bs
            JOIN FETCH bs.branch
            JOIN FETCH bs.specialty
            WHERE a.appointmentNumber = :number
            """)
    Optional<Appointment> findByAppointmentNumber(@Param("number") String number);

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.patient
            JOIN FETCH a.doctor
            JOIN FETCH a.branchSpecialty bs
            JOIN FETCH bs.branch
            JOIN FETCH bs.specialty
            WHERE a.appointmentNumber = :number AND a.status = :status
            """)
    Optional<Appointment> findByAppointmentNumberAndStatus(
            @Param("number") String number, @Param("status") AppointmentStatus status);

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.patient p
            JOIN FETCH a.doctor
            JOIN FETCH a.branchSpecialty bs
            JOIN FETCH bs.branch
            JOIN FETCH bs.specialty
            WHERE p.dpi = :dpi AND a.status = :status
            ORDER BY a.scheduledAt ASC
            """)
    List<Appointment> findByPatient_DpiAndStatus(
            @Param("dpi") String dpi, @Param("status") AppointmentStatus status);

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.patient p
            JOIN FETCH a.doctor
            JOIN FETCH a.branchSpecialty bs
            JOIN FETCH bs.branch
            JOIN FETCH bs.specialty
            WHERE p.dpi = :dpi AND a.status IN :statuses
            ORDER BY a.scheduledAt ASC
            """)
    List<Appointment> findByPatient_DpiAndStatusInOrderByScheduledAtAsc(
            @Param("dpi") String dpi, @Param("statuses") List<AppointmentStatus> statuses);

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.branchSpecialty bs
            JOIN FETCH bs.specialty
            WHERE a.patient.id = :patientId
            ORDER BY a.scheduledAt DESC
            """)
    List<Appointment> findByPatientIdWithDetails(@Param("patientId") Integer patientId);

    // ---- Fase 3: colas de trabajo (enfermería y médico).
    // ORDER BY a.priority ASC: la prioridad se guarda como texto y 'EMERGENCIA' < 'NORMAL',
    // así que las emergencias salen primero. Dentro de cada grupo, por orden de llegada.

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.patient
            JOIN FETCH a.doctor
            JOIN FETCH a.branchSpecialty bs
            JOIN FETCH bs.branch b
            JOIN FETCH bs.specialty
            WHERE a.status IN :statuses
              AND a.arrivalTime >= :since
              AND b.id = :branchId
            ORDER BY a.priority ASC, a.arrivalTime ASC
            """)
    List<Appointment> findQueueByBranch(@Param("statuses") Collection<AppointmentStatus> statuses,
                                        @Param("since") LocalDateTime since,
                                        @Param("branchId") Integer branchId);

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.patient
            JOIN FETCH a.doctor
            JOIN FETCH a.branchSpecialty bs
            JOIN FETCH bs.branch
            JOIN FETCH bs.specialty
            WHERE a.status IN :statuses
              AND a.arrivalTime >= :since
            ORDER BY a.priority ASC, a.arrivalTime ASC
            """)
    List<Appointment> findQueueAll(@Param("statuses") Collection<AppointmentStatus> statuses,
                                   @Param("since") LocalDateTime since);

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.patient
            JOIN FETCH a.doctor d
            JOIN FETCH a.branchSpecialty bs
            JOIN FETCH bs.branch
            JOIN FETCH bs.specialty
            WHERE d.id = :doctorId
              AND a.status IN :statuses
              AND a.arrivalTime >= :since
            ORDER BY a.priority ASC, a.arrivalTime ASC
            """)
    List<Appointment> findDoctorQueue(@Param("doctorId") Integer doctorId,
                                      @Param("statuses") Collection<AppointmentStatus> statuses,
                                      @Param("since") LocalDateTime since);

    // ---- Fase 3 (CU-12): recordatorios de citas de seguimiento

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.patient
            JOIN FETCH a.doctor
            JOIN FETCH a.branchSpecialty bs
            JOIN FETCH bs.branch
            JOIN FETCH bs.specialty
            WHERE a.followUpType IS NOT NULL
              AND a.reminderSent = false
              AND a.status NOT IN :excluded
              AND a.scheduledAt > :from
              AND a.scheduledAt <= :to
            """)
    List<Appointment> findFollowUpsPendingReminder(@Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to,
                                                   @Param("excluded") Collection<AppointmentStatus> excluded);

    // ---- Fase 5 (CU-16): citas visibles en el calendario del médico

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.patient
            JOIN FETCH a.branchSpecialty bs
            JOIN FETCH bs.specialty
            WHERE a.doctor.id = :doctorId
              AND a.status NOT IN :excluded
              AND a.scheduledAt >= :from
              AND a.scheduledAt < :to
            ORDER BY a.scheduledAt ASC
            """)
    List<Appointment> findForAgenda(@Param("doctorId") Integer doctorId,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to,
                                    @Param("excluded") Collection<AppointmentStatus> excluded);

    // ---- Consultas derivadas simples (no se muestran relaciones en pantalla)

    List<Appointment> findByPatientIdOrderByScheduledAtDesc(Integer patientId);

    List<Appointment> findByDoctorIdAndScheduledAtBetween(
            Integer doctorId, LocalDateTime from, LocalDateTime to);

    List<Appointment> findByStatusAndReservationExpiresAtBefore(
            AppointmentStatus status, LocalDateTime now);

    boolean existsByBranchSpecialty_Id(Integer branchSpecialtyId);
}
