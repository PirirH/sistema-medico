package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.BranchSpecialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BranchSpecialtyRepository extends JpaRepository<BranchSpecialty, Integer> {
    Optional<BranchSpecialty> findByBranch_IdAndSpecialty_IdAndActiveTrue(Integer branchId, Integer specialtyId);

    // ---- Fase 5 (CU-13)
    Optional<BranchSpecialty> findByBranch_IdAndSpecialty_Id(Integer branchId, Integer specialtyId);

    @Query("""
            SELECT bs FROM BranchSpecialty bs
            JOIN FETCH bs.branch
            JOIN FETCH bs.specialty
            ORDER BY bs.id ASC
            """)
    List<BranchSpecialty> findAllWithDetails();
}
