package com.hospital.sistemamedico.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/** Fila del detalle de despacho: lo recetado + la disponibilidad en la sucursal del farmacéutico. */
@Data
@AllArgsConstructor
public class DispensePreviewLine {
    private Integer prescriptionItemId;
    private String medicineName;
    private String unit;
    private boolean controlled;
    private String dose;
    private String frequency;
    private String duration;
    private String instructions;
    private Integer requestedQuantity;
    private BigDecimal unitPrice;
    /** null = el medicamento no tiene inventario registrado en la sucursal. */
    private Integer stock;
    private Integer minimumStock;
    /** Texto de alerta (FA01): "Sin inventario registrado" o "Stock bajo ..."; null si todo bien. */
    private String stockMessage;
}
