package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.InternalUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InternalUserRepository extends JpaRepository<InternalUser, Integer> {

    Optional<InternalUser> findByUsername(String username);

    Optional<InternalUser> findByDpi(String dpi);

    boolean existsByDpi(String dpi);

    boolean existsByUsername(String username);
}