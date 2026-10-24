package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.model.dto.NursingPanel;
import com.hospital.sistemamedico.model.dto.VitalSignsForm;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.entity.VitalSigns;

/** CU-07: Toma de signos vitales. */
public interface NursingService {

    NursingPanel getPanel(Integer nurseUserId);

    /** Paso 1: "Llamar y Tomar Signos". Pasa la cita de Paciente Presente a Signos Vitales. */
    Appointment callPatient(Integer appointmentId);

    /** Carga la cita para el formulario; debe estar en estado Signos Vitales. */
    Appointment loadForVitalSigns(Integer appointmentId);

    /** Pasos 9-12: valida, guarda y deja la cita En Espera. */
    VitalSigns register(Integer appointmentId, Integer nurseUserId, VitalSignsForm form);
}
