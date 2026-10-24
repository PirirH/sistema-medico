package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.model.dto.LabOrderForm;
import com.hospital.sistemamedico.model.dto.PrescriptionForm;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.entity.LabOrder;
import com.hospital.sistemamedico.model.entity.Prescription;

import java.util.List;

/** CU-08 FA01 (órdenes de laboratorio) y FA04 (recetas), desde la consulta del médico. */
public interface ClinicalOrderService {

    /** Carga la cita validando solo que pertenezca al médico (sirve para consultar resultados). */
    Appointment loadAppointment(Integer appointmentId, Integer doctorId);

    /** Carga la cita validando que sea del médico y que la consulta siga abierta. */
    Appointment loadForOrders(Integer appointmentId, Integer doctorId);

    LabOrder createLabOrder(Integer appointmentId, Integer doctorId, LabOrderForm form);

    Prescription createPrescription(Integer appointmentId, Integer doctorId, PrescriptionForm form);

    /** Órdenes del paciente de esa cita con sus resultados (para el médico y para seguimientos). */
    List<LabOrder> labOrdersOfPatient(Integer appointmentId, Integer doctorId);
}
