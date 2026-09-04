package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Integer> {
    List<Branch> findByActiveTrue();
}