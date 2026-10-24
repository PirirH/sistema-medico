package com.hospital.sistemamedico.model.dto;

import com.hospital.sistemamedico.model.enums.MovementType;
import lombok.Data;

import java.math.BigDecimal;

/** Formulario de movimiento manual de inventario (CU-15). Las reglas dependen del tipo: se validan en el servicio. */
@Data
public class InventoryMovementForm {
    private Integer medicineId;
    private Integer branchId;
    private MovementType movementType;
    private Integer quantity;
    private BigDecimal unitCost;
    private String referenceNumber;
    private String notes;
}
