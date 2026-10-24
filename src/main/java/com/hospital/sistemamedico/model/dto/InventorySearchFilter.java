package com.hospital.sistemamedico.model.dto;

import com.hospital.sistemamedico.model.enums.MovementType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Filtros del listado de la bitácora (CU-15 paso 2). Todos opcionales. */
@Data
public class InventorySearchFilter {
    private MovementType type;
    private Integer medicineId;
    private Integer branchId;
    private String reference;
    private String user;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;
}
