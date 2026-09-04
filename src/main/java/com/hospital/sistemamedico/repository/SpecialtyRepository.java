package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecialtyRepository extends JpaRepository<Specialty, Integer> {
    List<Specialty> findByActiveTrue();
}