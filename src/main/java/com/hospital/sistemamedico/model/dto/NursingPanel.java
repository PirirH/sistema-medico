package com.hospital.sistemamedico.model.dto;

import com.hospital.sistemamedico.model.entity.Appointment;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** Contenido del panel de enfermería (CU-07, paso 1). */
@Data
@AllArgsConstructor
public class NursingPanel {
    /** Pacientes en estado "Paciente Presente" (aún no llamados). */
    private List<Appointment> waiting;
    /** Pacientes ya llamados, en estado "Signos Vitales". */
    private List<Appointment> inProgress;
}
