package com.hospital.sistemamedico.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Orden de laboratorio que genera el médico (CU-08 FA01). */
@Data
public class LabOrderForm {
    private List<Integer> examIds = new ArrayList<>();
    private String notes;
    /** CU-09 FA01: el paciente hará los exámenes fuera. */
    private boolean external;
}
