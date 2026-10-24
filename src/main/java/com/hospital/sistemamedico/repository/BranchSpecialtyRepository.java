package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.BranchSpecialty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchSpecialtyRepository extends JpaRepository<BranchSpecialty, Integer> {

    Optional<BranchSpecialty> findByBranch_IdAndSpecialty_IdAndActiveTrue(Integer branchId, Integer specialtyId);
}