package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.model.dto.AppointmentRequestForm;
import com.hospital.sistemamedico.model.entity.Appointment;

public interface AppointmentSchedulingService {

    Appointment scheduleFromPortal(Integer patientId, AppointmentRequestForm form);
}
