package com.hospital.sistemamedico.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Sugerencia del autocompletado CIE-10 (se serializa a JSON). */
@Data
@AllArgsConstructor
public class Cie10Option {
    private String code;
    private String description;
}
