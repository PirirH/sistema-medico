package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.model.dto.FollowUpForm;
import com.hospital.sistemamedico.model.entity.Appointment;

/** CU-12: Agendamiento de cita de seguimiento. */
public interface FollowUpService {

    /** Pasos 2-3: carga la cita padre (con paciente, médico, especialidad y sucursal) validando que sea del médico. */
    Appointment loadParent(Integer parentAppointmentId, Integer doctorId);

    /** Pasos 7-10: valida, crea la cita, y envía la notificación por correo. */
    Appointment schedule(Integer parentAppointmentId, Integer doctorId, FollowUpForm form);
}
