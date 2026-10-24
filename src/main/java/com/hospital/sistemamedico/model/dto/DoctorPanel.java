package com.hospital.sistemamedico.model.dto;

import com.hospital.sistemamedico.model.entity.Appointment;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** Panel del médico (CU-08, paso 1): tres secciones. */
@Data
@AllArgsConstructor
public class DoctorPanel {
    private List<Appointment> waiting;
    private List<Appointment> inConsultation;
    private List<Appointment> evaluated;
}
