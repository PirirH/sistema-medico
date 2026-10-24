package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.LabExam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabExamRepository extends JpaRepository<LabExam, Integer> {
    List<LabExam> findByActiveTrueOrderByNameAsc();
}
