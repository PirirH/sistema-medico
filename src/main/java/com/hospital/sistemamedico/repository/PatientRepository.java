package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Integer> {

    Optional<Patient> findByDpi(String dpi);

    Optional<Patient> findByUsername(String username);

    boolean existsByDpi(String dpi);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}