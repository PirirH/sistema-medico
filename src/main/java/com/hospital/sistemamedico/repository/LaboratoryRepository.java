package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.Laboratory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LaboratoryRepository extends JpaRepository<Laboratory, Integer> {
    List<Laboratory> findByActiveTrueOrderByNameAsc();
}
