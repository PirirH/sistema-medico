package com.hospital.sistemamedico.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Catálogo CIE-10 (diagnósticos) para el autocompletado de CU-08. */
@Entity
@Table(name = "cie10")
@Data
@NoArgsConstructor
public class Cie10Code {

    @Id
    @Column(length = 10)
    private String code;

    @Column(nullable = false, length = 300)
    private String description;
}
