package com.hospital.sistemamedico.model.dto;

import lombok.Data;

import java.math.BigDecimal;

/** Formulario genérico de catálogos: cada catálogo usa solo los campos que le corresponden. */
@Data
public class CatalogForm {
    private String name;
    private String description;
    private String phone;
    private String address;
    private String openingHours;
    private BigDecimal price;
    private String unit;
    private boolean controlled;
    private Integer minimumStock;
    private String referenceRange;
    private Integer laboratoryId;
    private Integer medicineId;
    private Integer branchId;
    private Integer currentStock;
    private boolean active = true;
}
