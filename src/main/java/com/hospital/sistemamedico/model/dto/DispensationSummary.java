package com.hospital.sistemamedico.model.dto;

import com.hospital.sistemamedico.model.entity.Dispensation;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DispensationSummary {
    private Dispensation dispensation;
    /** Alertas de stock mínimo posteriores al despacho (CU-11 FA04). */
    private List<String> alerts;
}
