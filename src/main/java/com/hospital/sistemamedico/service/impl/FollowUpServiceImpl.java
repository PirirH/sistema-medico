package com.hospital.sistemamedico.service.impl;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.FollowUpEmailData;
import com.hospital.sistemamedico.model.dto.FollowUpForm;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.entity.Consultation;
import com.hospital.sistemamedico.model.enums.AppointmentChannel;
import com.hospital.sistemamedico.model.enums.AppointmentPriority;
import com.hospital.sistemamedico.model.enums.AppointmentStatus;
import com.hospital.sistemamedico.repository.AppointmentRepository;
import com.hospital.sistemamedico.repository.ConsultationRepository;
import com.hospital.sistemamedico.service.DoctorAvailabilityService;
import com.hospital.sistemamedico.service.EmailService;
import com.hospital.sistemamedico.service.FollowUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowUpServiceImpl implements FollowUpService {

    private static final String SLOT_TAKEN_MESSAGE =
            "El horario seleccionado ya no está disponible. Por favor, elija otro horario.";
    private static final String DATE_MESSAGE =
            "Seleccione una fecha futura dentro de los horarios disponibles del médico.";

    private final AppointmentRepository appointmentRepository;
    private final ConsultationRepository consultationRepository;
    private final DoctorAvailabilityService doctorAvailabilityService;
    private final EmailService emailService;

    @Value("${hospital.schedule.slot-duration-minutes}")
    private int slotDurationMinutes;

    @Override
    @Transactional(readOnly = true)
    public Appointment loadParent(Integer parentAppointmentId, Integer doctorId) {
        Appointment parent = appointmentRepository.findByIdWithDetails(parentAppointmentId)
                .orElseThrow(() -> new BusinessRuleException("Cita no encontrada."));

        if (!parent.getDoctor().getId().equals(doctorId)) {
            throw new BusinessRuleException("No tiene permiso para agendar seguimiento de una cita de otro médico.");
        }
        if (parent.getStatus() != AppointmentStatus.CONSULTA_MEDICA
                && parent.getStatus() != AppointmentStatus.ATENCION_FINALIZADA) {
            throw new BusinessRuleException(
                    "Debe existir una consulta médica activa o recién finalizada para agendar un seguimiento.");
        }
        return parent;
    }

    @Override
    @Transactional
    public Appointment schedule(Integer parentAppointmentId, Integer doctorId, FollowUpForm form) {
        Appointment parent = loadParent(parentAppointmentId, doctorId);
        Consultation consultation = consultationRepository.findByAppointment_Id(parentAppointmentId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Debe existir una consulta médica activa o recién finalizada para agendar un seguimiento."));

        if (form.getAppointmentDate() == null || form.getAppointmentTime() == null) {
            throw new BusinessRuleException(DATE_MESSAGE);
        }
        LocalDateTime scheduledAt = LocalDateTime.of(form.getAppointmentDate(), form.getAppointmentTime());
        if (!scheduledAt.isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException(DATE_MESSAGE);
        }

        // FA01: el horario pudo ser ocupado entre la selección y la confirmación.
        List<LocalTime> free = doctorAvailabilityService.getAvailableSlots(doctorId, form.getAppointmentDate());
        if (!free.contains(form.getAppointmentTime())) {
            throw new BusinessRuleException(SLOT_TAKEN_MESSAGE);
        }

        Appointment followUp = new Appointment();
        followUp.setAppointmentNumber("TMP-" + UUID.randomUUID().toString().substring(0, 15));
        followUp.setPatient(parent.getPatient());
        followUp.setDoctor(parent.getDoctor());
        followUp.setBranchSpecialty(parent.getBranchSpecialty());
        followUp.setScheduledAt(scheduledAt);
        followUp.setDurationMinutes(slotDurationMinutes);
        // Decisión de diseño: el seguimiento nace CONFIRMADA y sin costo (Q0.00), porque es continuación
        // de una atención ya pagada. Si se quisiera cobrar, bastaría con usar PENDIENTE_PAGO y el precio.
        followUp.setStatus(AppointmentStatus.CONFIRMADA);
        followUp.setChannel(AppointmentChannel.PRESENCIAL);
        followUp.setPriority(form.getPriority() == null ? AppointmentPriority.NORMAL : form.getPriority());
        followUp.setReason(form.getReason().trim());
        followUp.setTotalAmount(BigDecimal.ZERO);
        followUp.setFollowUpType(form.getFollowUpType());
        followUp.setParentConsultationId(consultation.getId());

        Appointment saved = appointmentRepository.save(followUp);
        saved.setAppointmentNumber("CIT-" + Year.now().getValue() + "-" + String.format("%06d", saved.getId()));
        saved = appointmentRepository.save(saved);

        // RN-CU11-04: notificación por correo (un fallo de correo nunca revierte el agendamiento).
        emailService.sendFollowUpNotification(FollowUpEmailData.builder()
                .to(parent.getPatient().getEmail())
                .patientName(parent.getPatient().getFullName())
                .appointmentNumber(saved.getAppointmentNumber())
                .doctorName(parent.getDoctor().getFullName())
                .branch(parent.getBranchSpecialty().getBranch().getName())
                .followUpTypeLabel(form.getFollowUpType().getLabel())
                .observations(saved.getReason())
                .scheduledAt(saved.getScheduledAt())
                .build());

        return saved;
    }
}
