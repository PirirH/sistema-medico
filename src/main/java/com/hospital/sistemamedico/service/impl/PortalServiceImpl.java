package com.hospital.sistemamedico.service.impl;

import com.hospital.sistemamedico.model.entity.Branch;
import com.hospital.sistemamedico.model.entity.Specialty;
import com.hospital.sistemamedico.repository.BranchRepository;
import com.hospital.sistemamedico.repository.InternalUserRepository;
import com.hospital.sistemamedico.repository.PatientRepository;
import com.hospital.sistemamedico.repository.SpecialtyRepository;
import com.hospital.sistemamedico.service.PortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PortalServiceImpl implements PortalService {

    private final BranchRepository branchRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PatientRepository patientRepository;
    private final InternalUserRepository internalUserRepository;

    @Override
    public List<Branch> getActiveBranches() {
        return branchRepository.findByActiveTrue();
    }

    @Override
    public List<Specialty> getActiveSpecialties() {
        return specialtyRepository.findByActiveTrue();
    }

    @Override
    public DpiCheckResult checkDpi(String dpi) {
        // CU-00 paso 8: confirma que está registrado como paciente
        if (patientRepository.existsByDpi(dpi)) {
            return DpiCheckResult.REGISTERED_PATIENT;
        }
        // FA04: el DPI pertenece a un usuario interno del sistema
        if (internalUserRepository.existsByDpi(dpi)) {
            return DpiCheckResult.BELONGS_TO_INTERNAL_USER;
        }
        // FA03: no se encuentra registro asociado
        return DpiCheckResult.NOT_REGISTERED;
    }
}