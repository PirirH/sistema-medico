package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.Cie10Code;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Cie10Repository extends JpaRepository<Cie10Code, String> {

    List<Cie10Code> findTop10ByCodeStartingWithIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByCodeAsc(
            String code, String description);
}
