package com.hospital.sistemamedico.model.dto;

import com.hospital.sistemamedico.model.entity.Prescription;
import lombok.AllArgsConstructor;
import lombok.Data;

/** Fila de resultados de búsqueda de recetas (CU-11 paso 2), con su vigencia calculada. */
@Data
@AllArgsConstructor
public class PrescriptionRow {
    private Prescription prescription;
    private long daysSinceIssued;
    private boolean expired;
}
