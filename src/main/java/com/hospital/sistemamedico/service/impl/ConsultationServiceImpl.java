package com.hospital.sistemamedico.service.impl;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.Cie10Option;
import com.hospital.sistemamedico.model.dto.ConsultationDetail;
import com.hospital.sistemamedico.model.dto.ConsultationForm;
import com.hospital.sistemamedico.model.dto.DoctorPanel;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.entity.Consultation;
import com.hospital.sistemamedico.model.enums.AppointmentStatus;
import com.hospital.sistemamedico.model.enums.ConsultationStatus;
import com.hospital.sistemamedico.repository.AppointmentRepository;
import com.hospital.sistemamedico.repository.Cie10Repository;
import com.hospital.sistemamedico.repository.ConsultationRepository;
import com.hospital.sistemamedico.repository.VitalSignsRepository;
import com.hospital.sistemamedico.service.ConsultationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private final AppointmentRepository appointmentRepository;
    private final ConsultationRepository consultationRepository;
    private final VitalSignsRepository vitalSignsRepository;
    private final Cie10Repository cie10Repository;

    @Override
    @Transactional(readOnly = true)
    public DoctorPanel getPanel(Integer doctorId) {
        LocalDateTime since = LocalDate.now().atStartOfDay();
        List<Appointment> appointments = appointmentRepository.findDoctorQueue(
                doctorId, List.of(AppointmentStatus.EN_ESPERA, AppointmentStatus.CONSULTA_MEDICA), since);

        List<Integer> ids = appointments.stream().map(Appointment::getId).collect(Collectors.toList());
        Map<Integer, ConsultationStatus> consultationStatus = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Consultation c : consultationRepository.findByAppointment_IdIn(ids)) {
                consultationStatus.put(c.getAppointment().getId(), c.getStatus());
            }
        }

        List<Appointment> waiting = new ArrayList<>();
        List<Appointment> inConsultation = new ArrayList<>();
        List<Appointment> evaluated = new ArrayList<>();
        for (Appointment a : appointments) {
            if (a.getStatus() == AppointmentStatus.EN_ESPERA) {
                waiting.add(a);
            } else if (consultationStatus.get(a.getId()) == ConsultationStatus.FINALIZADA) {
                evaluated.add(a);
            } else {
                inConsultation.add(a);
            }
        }
        return new DoctorPanel(waiting, inConsultation, evaluated);
    }

    @Override
    @Transactional
    public Appointment startConsultation(Integer appointmentId, Integer doctorId) {
        Appointment appointment = loadOwned(appointmentId, doctorId);

        if (appointment.getStatus() != AppointmentStatus.EN_ESPERA) {
            throw new BusinessRuleException(
                    "La cita no está 'En Espera'. Es posible que ya haya sido atendida o actualizada por otro usuario.");
        }

        appointment.setStatus(AppointmentStatus.CONSULTA_MEDICA);
        appointment = appointmentRepository.save(appointment);

        if (consultationRepository.findByAppointment_Id(appointmentId).isEmpty()) {
            Consultation consultation = new Consultation();
            consultation.setAppointment(appointment);
            consultation.setVisitReason(appointment.getReason());
            consultationRepository.save(consultation);
        }
        return appointment;
    }

    @Override
    @Transactional
    public Appointment markNoShow(Integer appointmentId, Integer doctorId) {
        Appointment appointment = loadOwned(appointmentId, doctorId);

        if (appointment.getStatus() != AppointmentStatus.EN_ESPERA) {
            throw new BusinessRuleException("Solo se puede marcar 'No Asistió' a pacientes en espera.");
        }

        appointment.setStatus(AppointmentStatus.NO_ASISTIO);
        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationDetail load(Integer appointmentId, Integer doctorId) {
        Appointment appointment = loadOwned(appointmentId, doctorId);

        if (appointment.getStatus() != AppointmentStatus.CONSULTA_MEDICA
                && appointment.getStatus() != AppointmentStatus.ATENCION_FINALIZADA) {
            throw new BusinessRuleException("La cita no tiene una consulta médica en curso.");
        }

        Consultation consultation = consultationRepository.findByAppointment_Id(appointmentId)
                .orElseThrow(() -> new BusinessRuleException(
                        "La consulta aún no ha sido iniciada. Use 'Iniciar Consulta' en el panel."));

        return new ConsultationDetail(appointment, consultation,
                vitalSignsRepository.findByAppointment_Id(appointmentId).orElse(null));
    }

    @Override
    @Transactional
    public ConsultationDetail save(Integer appointmentId, Integer doctorId, ConsultationForm form) {
        ConsultationDetail detail = load(appointmentId, doctorId);
        Consultation consultation = detail.getConsultation();

        if (detail.getAppointment().getStatus() != AppointmentStatus.CONSULTA_MEDICA
                || consultation.getStatus() == ConsultationStatus.FINALIZADA) {
            throw new BusinessRuleException("La consulta ya fue finalizada y no puede modificarse.");
        }

        consultation.setVisitReason(clean(form.getVisitReason()));
        consultation.setClinicalFindings(clean(form.getClinicalFindings()));
        consultation.setCie10Code(clean(form.getCie10Code()) == null ? null : clean(form.getCie10Code()).toUpperCase());
        consultation.setDiagnosis(clean(form.getDiagnosis()));
        consultation.setTreatmentPlan(clean(form.getTreatmentPlan()));
        consultation.setNotes(clean(form.getNotes()));
        consultation.setUpdatedAt(LocalDateTime.now());

        if (consultation.getVisitReason() == null) {
            throw new BusinessRuleException("El motivo de visita es obligatorio.");
        }
        if (consultation.getCie10Code() != null && !cie10Repository.existsById(consultation.getCie10Code())) {
            throw new BusinessRuleException(
                    "El código CIE-10 ingresado no existe en el catálogo. Selecciónelo de las sugerencias.");
        }

        if (form.getStatus() == ConsultationStatus.FINALIZADA) {
            validateForClose(consultation);
            consultation.setStatus(ConsultationStatus.FINALIZADA);
            consultation.setFinishedAt(LocalDateTime.now());
        }

        consultationRepository.save(consultation);
        return detail;
    }

    @Override
    @Transactional
    public Appointment finalizeAttention(Integer appointmentId, Integer doctorId) {
        ConsultationDetail detail = load(appointmentId, doctorId);

        if (detail.getAppointment().getStatus() != AppointmentStatus.CONSULTA_MEDICA) {
            throw new BusinessRuleException("La atención de esta cita ya fue finalizada.");
        }
        if (detail.getConsultation().getStatus() != ConsultationStatus.FINALIZADA) {
            throw new BusinessRuleException("Debe finalizar la consulta antes de finalizar la atención.");
        }

        Appointment appointment = detail.getAppointment();
        appointment.setStatus(AppointmentStatus.ATENCION_FINALIZADA);
        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cie10Option> searchCie10(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        String q = query.trim();
        return cie10Repository
                .findTop10ByCodeStartingWithIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByCodeAsc(q, q)
                .stream()
                .map(c -> new Cie10Option(c.getCode(), c.getDescription()))
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ auxiliares

    private void validateForClose(Consultation c) {
        // FA05: sin diagnóstico no se puede finalizar.
        if (c.getDiagnosis() == null) {
            throw new BusinessRuleException(
                    "No es posible finalizar la consulta sin registrar un diagnóstico. El campo Diagnóstico es obligatorio.");
        }
        // RN-CU08-01
        if (c.getDiagnosis().length() < 10 || c.getDiagnosis().length() > 5000) {
            throw new BusinessRuleException(
                    "El diagnóstico es obligatorio. Debe contener entre 10 y 5000 caracteres.");
        }
        // RN-CU08-02
        if (c.getVisitReason() == null || c.getClinicalFindings() == null || c.getTreatmentPlan() == null) {
            throw new BusinessRuleException("Debe completar todos los campos obligatorios para cerrar la consulta.");
        }
    }

    private Appointment loadOwned(Integer appointmentId, Integer doctorId) {
        Appointment appointment = appointmentRepository.findByIdWithDetails(appointmentId)
                .orElseThrow(() -> new BusinessRuleException("Cita no encontrada."));
        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new BusinessRuleException("No tiene permiso para atender esta cita: está asignada a otro médico.");
        }
        return appointment;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
