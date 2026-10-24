package com.hospital.sistemamedico.model.dto;

import com.hospital.sistemamedico.model.entity.Appointment;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReceptionSearchResult {

    public enum Outcome {
        CITA_ENCONTRADA,
        PACIENTE_NO_REGISTRADO,
        PACIENTE_SIN_CITAS_ACTIVAS
    }

    private Outcome outcome;
    private Appointment appointment; // solo tiene valor cuando outcome == CITA_ENCONTRADA
}