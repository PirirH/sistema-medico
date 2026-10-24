package com.hospital.sistemamedico.model.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Resultado de un examen (CU-09 paso 9). */
@Data
public class LabResultForm {
    private String resultValue;
    private String resultUnit;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate resultDate;
    /** Marcado manual "Fuera de rango" (CU-09 FA02). */
    private boolean outOfRange;
    private String resultNotes;
}
