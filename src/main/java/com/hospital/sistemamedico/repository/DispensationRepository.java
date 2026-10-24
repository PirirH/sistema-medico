package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.Dispensation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DispensationRepository extends JpaRepository<Dispensation, Integer> {

    @Query("""
            SELECT DISTINCT d FROM Dispensation d
            JOIN FETCH d.prescription p
            JOIN FETCH p.patient
            JOIN FETCH d.branch
            JOIN FETCH d.pharmacist
            LEFT JOIN FETCH d.items i
            LEFT JOIN FETCH i.medicine
            WHERE d.id = :id
            """)
    Optional<Dispensation> findByIdWithDetails(@Param("id") Integer id);
}
