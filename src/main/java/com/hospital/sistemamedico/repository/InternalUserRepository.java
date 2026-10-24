package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.InternalUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InternalUserRepository extends JpaRepository<InternalUser, Integer> {
    Optional<InternalUser> findByUsername(String username);
    Optional<InternalUser> findByDpi(String dpi);
    boolean existsByDpi(String dpi);
    boolean existsByUsername(String username);
    List<InternalUser> findByRole_NameAndBranch_IdAndSpecialty_IdAndActiveTrue(
            String roleName, Integer branchId, Integer specialtyId);

    // ---- Fase 5 (CU-01)
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);
    boolean existsByDpiAndIdNot(String dpi, Integer id);

    @Query("""
            SELECT u FROM InternalUser u
            JOIN FETCH u.role
            LEFT JOIN FETCH u.branch
            LEFT JOIN FETCH u.specialty
            WHERE u.deleted = false
            ORDER BY u.id ASC
            """)
    List<InternalUser> findAllNotDeleted();

    @Query("""
            SELECT u FROM InternalUser u
            JOIN FETCH u.role
            LEFT JOIN FETCH u.branch
            LEFT JOIN FETCH u.specialty
            WHERE u.id = :id AND u.deleted = false
            """)
    Optional<InternalUser> findByIdNotDeleted(Integer id);
}
