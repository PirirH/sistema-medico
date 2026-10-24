package com.hospital.sistemamedico.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CatalogRow {
    private Integer id;
    private String name;
    private String detail;
    private boolean active;
}
