package com.hospital.sistemamedico.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BranchSpecialtyForm {
    private Integer branchId;
    private Integer specialtyId;
    private BigDecimal price;
}
