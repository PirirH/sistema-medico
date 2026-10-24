package com.hospital.sistemamedico.model.dto;

import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.entity.Consultation;
import com.hospital.sistemamedico.model.entity.VitalSigns;
import lombok.AllArgsConstructor;
import lombok.Data;

/** Datos para pintar el formulario de consulta (cita + consulta + signos vitales del paciente). */
@Data
@AllArgsConstructor
public class ConsultationDetail {
    private Appointment appointment;
    private Consultation consultation;
    private VitalSigns vitalSigns; // puede ser null si no existen
}
