package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.model.dto.ReceptionSearchResult;
import com.hospital.sistemamedico.model.entity.Appointment;

public interface ReceptionService {

    ReceptionSearchResult search(String appointmentNumber, String dpi);

    Appointment registerArrival(Integer appointmentId, Long expectedVersion);

    Appointment reassignDoctor(Integer appointmentId, Integer newDoctorId);
}