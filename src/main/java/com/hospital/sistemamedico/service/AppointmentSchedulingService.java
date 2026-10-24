package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.model.dto.AppointmentRequestForm;
import com.hospital.sistemamedico.model.dto.WalkInAppointmentForm;
import com.hospital.sistemamedico.model.entity.Appointment;

public interface AppointmentSchedulingService {

    /** Cita agendada por el propio paciente desde el portal (CU-03). */
    Appointment scheduleFromPortal(Integer patientId, AppointmentRequestForm form);

    /** Cita creada por recepción para un paciente presente, o registro de emergencia (CU-05). */
    Appointment scheduleWalkIn(WalkInAppointmentForm form);
}
