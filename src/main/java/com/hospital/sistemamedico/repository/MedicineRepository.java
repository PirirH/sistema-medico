package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicineRepository extends JpaRepository<Medicine, Integer> {
    List<Medicine> findByActiveTrueOrderByNameAsc();
}
