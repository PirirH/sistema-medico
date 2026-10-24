package com.hospital.sistemamedico.service.impl;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.LabOrderForm;
import com.hospital.sistemamedico.model.dto.PrescriptionForm;
import com.hospital.sistemamedico.model.dto.PrescriptionItemForm;
import com.hospital.sistemamedico.model.entity.*;
import com.hospital.sistemamedico.model.enums.AppointmentStatus;
import com.hospital.sistemamedico.model.enums.LabOrderStatus;
import com.hospital.sistemamedico.model.enums.PrescriptionStatus;
import com.hospital.sistemamedico.repository.*;
import com.hospital.sistemamedico.service.ClinicalOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Year;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicalOrderServiceImpl implements ClinicalOrderService {

    private static final String PRESCRIPTION_MESSAGE =
            "Todos los campos de la receta son obligatorios excepto indicaciones especiales.";

    private final AppointmentRepository appointmentRepository;
    private final ConsultationRepository consultationRepository;
    private final LabExamRepository labExamRepository;
    private final LabOrderRepository labOrderRepository;
    private final MedicineRepository medicineRepository;
    private final PrescriptionRepository prescriptionRepository;

    @Override
    @Transactional(readOnly = true)
    public Appointment loadAppointment(Integer appointmentId, Integer doctorId) {
        return loadOwned(appointmentId, doctorId);
    }

    @Override
    @Transactional(readOnly = true)
    public Appointment loadForOrders(Integer appointmentId, Integer doctorId) {
        Appointment appointment = loadOwned(appointmentId, doctorId);
        if (appointment.getStatus() != AppointmentStatus.CONSULTA_MEDICA) {
            throw new BusinessRuleException(
                    "Solo se pueden generar órdenes y recetas mientras la cita está en Consulta Médica.");
        }
        return appointment;
    }

    @Override
    @Transactional
    public LabOrder createLabOrder(Integer appointmentId, Integer doctorId, LabOrderForm form) {
        Appointment appointment = loadForOrders(appointmentId, doctorId);

        Set<Integer> examIds = new LinkedHashSet<>();
        if (form.getExamIds() != null) {
            form.getExamIds().stream().filter(id -> id != null).forEach(examIds::add);
        }
        if (examIds.isEmpty()) {
            throw new BusinessRuleException("Debe seleccionar al menos un examen de laboratorio.");
        }

        LabOrder order = new LabOrder();
        order.setOrderNumber("TMP-" + UUID.randomUUID().toString().substring(0, 15));
        order.setAppointment(appointment);
        order.setPatient(appointment.getPatient());
        order.setDoctor(appointment.getDoctor());
        order.setBranch(appointment.getBranchSpecialty().getBranch());
        order.setStatus(LabOrderStatus.PENDIENTE);
        order.setExternal(form.isExternal());
        order.setNotes(clean(form.getNotes()));

        BigDecimal total = BigDecimal.ZERO;
        for (Integer examId : examIds) {
            LabExam exam = labExamRepository.findById(examId)
                    .filter(LabExam::isActive)
                    .orElseThrow(() -> new BusinessRuleException("Uno de los exámenes seleccionados no está disponible."));
            LabOrderItem item = new LabOrderItem();
            item.setLabOrder(order);
            item.setExam(exam);
            item.setAmount(exam.getDefaultAmount());
            order.getItems().add(item);
            total = total.add(exam.getDefaultAmount());
        }
        order.setTotalAmount(total);

        LabOrder saved = labOrderRepository.save(order);
        saved.setOrderNumber("OL-" + Year.now().getValue() + "-" + String.format("%06d", saved.getId()));
        return labOrderRepository.save(saved);
    }

    @Override
    @Transactional
    public Prescription createPrescription(Integer appointmentId, Integer doctorId, PrescriptionForm form) {
        Appointment appointment = loadForOrders(appointmentId, doctorId);

        // Las filas completamente vacías se ignoran (el formulario siempre trae una fila libre).
        List<PrescriptionItemForm> rows = form.getItems() == null ? List.of() : form.getItems().stream()
                .filter(r -> r != null && (r.getMedicineId() != null || notBlank(r.getDose())
                        || notBlank(r.getFrequency()) || notBlank(r.getDuration())))
                .toList();
        if (rows.isEmpty()) {
            throw new BusinessRuleException("Debe agregar al menos un medicamento a la receta.");
        }

        Prescription prescription = new Prescription();
        prescription.setPrescriptionNumber("TMP-" + UUID.randomUUID().toString().substring(0, 15));
        prescription.setAppointment(appointment);
        prescription.setPatient(appointment.getPatient());
        prescription.setDoctor(appointment.getDoctor());
        prescription.setBranch(appointment.getBranchSpecialty().getBranch());
        prescription.setStatus(PrescriptionStatus.ACTIVA);
        prescription.setNotes(clean(form.getNotes()));
        consultationRepository.findByAppointment_Id(appointmentId)
                .ifPresent(c -> prescription.setConsultationId(c.getId()));

        for (PrescriptionItemForm row : rows) {
            if (row.getMedicineId() == null || !notBlank(row.getDose()) || !notBlank(row.getFrequency())
                    || !notBlank(row.getDuration()) || row.getQuantity() == null || row.getQuantity() < 1) {
                throw new BusinessRuleException(PRESCRIPTION_MESSAGE);
            }
            Medicine medicine = medicineRepository.findById(row.getMedicineId())
                    .filter(Medicine::isActive)
                    .orElseThrow(() -> new BusinessRuleException("Uno de los medicamentos seleccionados no está disponible."));

            PrescriptionItem item = new PrescriptionItem();
            item.setPrescription(prescription);
            item.setMedicine(medicine);
            item.setDose(row.getDose().trim());
            item.setFrequency(row.getFrequency().trim());
            item.setDuration(row.getDuration().trim());
            item.setInstructions(clean(row.getInstructions()));
            item.setQuantity(row.getQuantity());
            prescription.getItems().add(item);
        }

        Prescription saved = prescriptionRepository.save(prescription);
        saved.setPrescriptionNumber("RX-" + Year.now().getValue() + "-" + String.format("%06d", saved.getId()));
        return prescriptionRepository.save(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabOrder> labOrdersOfPatient(Integer appointmentId, Integer doctorId) {
        Appointment appointment = loadOwned(appointmentId, doctorId);
        return labOrderRepository.findByPatientWithItems(appointment.getPatient().getId());
    }

    // ------------------------------------------------------------------ auxiliares

    private Appointment loadOwned(Integer appointmentId, Integer doctorId) {
        Appointment appointment = appointmentRepository.findByIdWithDetails(appointmentId)
                .orElseThrow(() -> new BusinessRuleException("Cita no encontrada."));
        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new BusinessRuleException("No tiene permiso para esta cita: está asignada a otro médico.");
        }
        return appointment;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String clean(String s) {
        return notBlank(s) ? s.trim() : null;
    }
}
