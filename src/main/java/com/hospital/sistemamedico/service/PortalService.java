package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.model.entity.Branch;
import com.hospital.sistemamedico.model.entity.Specialty;

import java.util.List;

public interface PortalService {

    List<Branch> getActiveBranches();

    List<Specialty> getActiveSpecialties();

    enum DpiCheckResult { REGISTERED_PATIENT, NOT_REGISTERED, BELONGS_TO_INTERNAL_USER }

    DpiCheckResult checkDpi(String dpi);
}