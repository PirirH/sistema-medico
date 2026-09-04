package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.model.dto.PatientRegistrationForm;
import com.hospital.sistemamedico.model.entity.Patient;

public interface PatientService {

    /**
     * Registra un nuevo paciente (CU-02). Valida unicidad de DPI, username y
     * correo, y almacena la contraseña con hash.
     */
    Patient register(PatientRegistrationForm form);
}