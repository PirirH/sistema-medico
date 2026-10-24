package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.Prescription;
import com.hospital.sistemamedico.model.enums.PrescriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Integer> {

    @Query("""
            SELECT DISTINCT p FROM Prescription p
            JOIN FETCH p.patient
            JOIN FETCH p.doctor
            JOIN FETCH p.branch
            LEFT JOIN FETCH p.items i
            LEFT JOIN FETCH i.medicine
            WHERE p.id = :id
            """)
    Optional<Prescription> findByIdWithDetails(@Param("id") Integer id);

    /** CU-11 paso 2: solo recetas activas. Los parámetros ausentes llegan como "" o -1. */
    @Query("""
            SELECT p FROM Prescription p
            JOIN FETCH p.patient
            JOIN FETCH p.doctor
            WHERE p.status = :status
              AND (p.prescriptionNumber = :number OR p.id = :id OR p.consultationId = :consultationId)
            ORDER BY p.createdAt DESC
            """)
    List<Prescription> search(@Param("status") PrescriptionStatus status,
                              @Param("number") String number,
                              @Param("id") Integer id,
                              @Param("consultationId") Integer consultationId);

    List<Prescription> findByAppointment_IdOrderByCreatedAtDesc(Integer appointmentId);
}
