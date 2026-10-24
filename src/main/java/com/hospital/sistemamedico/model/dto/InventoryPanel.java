package com.hospital.sistemamedico.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Panel informativo en tiempo real del formulario de movimientos (RN-CU13-03). Se serializa a JSON. */
@Data
@AllArgsConstructor
public class InventoryPanel {
    private boolean exists;
    private int currentStock;
    private Integer minimumStock;
}
