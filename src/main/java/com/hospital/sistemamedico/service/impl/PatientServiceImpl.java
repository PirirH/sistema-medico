package com.hospital.sistemamedico.service.impl;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.PatientRegistrationForm;
import com.hospital.sistemamedico.model.entity.Patient;
import com.hospital.sistemamedico.repository.PatientRepository;
import com.hospital.sistemamedico.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Patient register(PatientRegistrationForm form) {

        // RN-CU02-05: nombre de usuario único
        if (patientRepository.existsByUsername(form.getUsername())) {
            throw new BusinessRuleException(
                    "El nombre de usuario " + form.getUsername() + " ya se encuentra registrado. Por favor, elija otro.");
        }
        // RNF-027: unicidad de DPI y correo
        if (patientRepository.existsByDpi(form.getDpi())) {
            throw new BusinessRuleException(
                    "Ya existe un registro asociado a este DPI.");
        }
        if (patientRepository.existsByEmail(form.getEmail())) {
            throw new BusinessRuleException(
                    "Ya existe una cuenta registrada con este correo electrónico.");
        }

        Patient patient = new Patient();
        patient.setDpi(form.getDpi());
        patient.setFullName(form.getFullName());
        patient.setPhone(form.getPhone());
        patient.setEmail(form.getEmail());
        patient.setInsuranceNumber(form.getInsuranceNumber());
        patient.setUsername(form.getUsername());
        patient.setPassword(passwordEncoder.encode(form.getPassword()));
        patient.setActive(true);

        return patientRepository.save(patient);
    }
}