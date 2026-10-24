package com.hospital.sistemamedico.model.dto;

import lombok.Data;

/** Una fila de la receta (RN-CU08-03). */
@Data
public class PrescriptionItemForm {
    private Integer medicineId;
    private String dose;
    private String frequency;
    private String duration;
    private String instructions; // opcional
    private Integer quantity = 1;
}
