package com.hospital.sistemamedico.model.dto;

import lombok.Data;

/** Una línea del despacho (CU-11 pasos 6-7). quantity = 0 significa "no despachar este medicamento". */
@Data
public class DispenseLineForm {
    private Integer prescriptionItemId;
    private Integer quantity = 0;
    private boolean substituted;
    private String substitutionReason;
    private String substituteName;
    /** RNF-017: autorización adicional para medicamentos controlados. */
    private boolean controlledAuthorized;
}
