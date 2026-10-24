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

    Optional<Appointment> findByAppointmentNumber(String appointmentNumber);

    List<Appointment> findByPatientIdOrderByScheduledAtDesc(Integer patientId);

    Optional<Appointment> findByAppointmentNumberAndStatus(String appointmentNumber, AppointmentStatus status);

    List<Appointment> findByDoctorIdAndScheduledAtBetween(
            Integer doctorId, LocalDateTime from, LocalDateTime to);

    List<Appointment> findByStatusAndReservationExpiresAtBefore(
            AppointmentStatus status, LocalDateTime now);

    List<Appointment> findByPatient_DpiAndStatus(String dpi, AppointmentStatus status);

    List<Appointment> findByPatient_DpiAndStatusInOrderByScheduledAtAsc(String dpi, List<AppointmentStatus> statuses);

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
}