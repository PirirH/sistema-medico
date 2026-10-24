package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.model.dto.Cie10Option;
import com.hospital.sistemamedico.model.dto.ConsultationDetail;
import com.hospital.sistemamedico.model.dto.ConsultationForm;
import com.hospital.sistemamedico.model.dto.DoctorPanel;
import com.hospital.sistemamedico.model.entity.Appointment;

import java.util.List;

/** CU-08: Consulta médica. */
public interface ConsultationService {

    DoctorPanel getPanel(Integer doctorId);

    /** Paso 2: "Iniciar Consulta" (EN_ESPERA -> CONSULTA_MEDICA). */
    Appointment startConsultation(Integer appointmentId, Integer doctorId);

    /** FA06: "No Asistió". */
    Appointment markNoShow(Integer appointmentId, Integer doctorId);

    ConsultationDetail load(Integer appointmentId, Integer doctorId);

    /** Pasos 4-9 y FA05. Devuelve el detalle ya actualizado. */
    ConsultationDetail save(Integer appointmentId, Integer doctorId, ConsultationForm form);

    /** Paso 11: "Finalizar Atención". */
    Appointment finalizeAttention(Integer appointmentId, Integer doctorId);

    /** Autocompletado CIE-10 (RNF-004). */
    List<Cie10Option> searchCie10(String query);
}
