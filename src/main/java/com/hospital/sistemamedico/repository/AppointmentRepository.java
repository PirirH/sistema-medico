package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    // ---- Consultas derivadas simples (no se muestran relaciones en pantalla)

    List<Appointment> findByPatientIdOrderByScheduledAtDesc(Integer patientId);

    List<Appointment> findByDoctorIdAndScheduledAtBetween(
            Integer doctorId, LocalDateTime from, LocalDateTime to);

    List<Appointment> findByStatusAndReservationExpiresAtBefore(
            AppointmentStatus status, LocalDateTime now);
}
