package com.hospital.sistemamedico.service.impl;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.NursingPanel;
import com.hospital.sistemamedico.model.dto.VitalSignsForm;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.entity.InternalUser;
import com.hospital.sistemamedico.model.entity.VitalSigns;
import com.hospital.sistemamedico.model.enums.AppointmentPriority;
import com.hospital.sistemamedico.model.enums.AppointmentStatus;
import com.hospital.sistemamedico.repository.AppointmentRepository;
import com.hospital.sistemamedico.repository.InternalUserRepository;
import com.hospital.sistemamedico.repository.VitalSignsRepository;
import com.hospital.sistemamedico.service.NursingService;
import com.hospital.sistemamedico.service.VitalSignAlertEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NursingServiceImpl implements NursingService {

    private final AppointmentRepository appointmentRepository;
    private final VitalSignsRepository vitalSignsRepository;
    private final InternalUserRepository internalUserRepository;

    @Override
    @Transactional(readOnly = true)
    public NursingPanel getPanel(Integer nurseUserId) {
        Integer branchId = branchOf(nurseUserId);
        LocalDateTime since = LocalDate.now().atStartOfDay();

        List<Appointment> waiting = queue(List.of(AppointmentStatus.PACIENTE_PRESENTE), since, branchId);
        List<Appointment> inProgress = queue(List.of(AppointmentStatus.SIGNOS_VITALES), since, branchId);
        return new NursingPanel(waiting, inProgress);
    }

    @Override
    @Transactional
    public Appointment callPatient(Integer appointmentId) {
        Appointment appointment = load(appointmentId);

        if (appointment.getStatus() != AppointmentStatus.PACIENTE_PRESENTE) {
            throw new BusinessRuleException(
                    "El paciente ya no está en estado 'Paciente Presente'. Es posible que otro usuario ya lo haya llamado.");
        }

        appointment.setStatus(AppointmentStatus.SIGNOS_VITALES);
        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public Appointment loadForVitalSigns(Integer appointmentId) {
        Appointment appointment = load(appointmentId);
        if (appointment.getStatus() != AppointmentStatus.SIGNOS_VITALES) {
            throw new BusinessRuleException(
                    "Solo se pueden registrar signos vitales de pacientes llamados (estado 'Signos Vitales').");
        }
        return appointment;
    }

    @Override
    @Transactional
    public VitalSigns register(Integer appointmentId, Integer nurseUserId, VitalSignsForm form) {
        Appointment appointment = load(appointmentId);

        if (appointment.getStatus() != AppointmentStatus.SIGNOS_VITALES) {
            throw new BusinessRuleException(
                    "Solo se pueden registrar signos vitales de pacientes llamados (estado 'Signos Vitales').");
        }
        // La presión sistólica siempre es mayor que la diastólica.
        if (form.getSystolic() <= form.getDiastolic()) {
            throw new BusinessRuleException(VitalSignsForm.PA_MESSAGE);
        }

        InternalUser nurse = internalUserRepository.findById(nurseUserId)
                .orElseThrow(() -> new BusinessRuleException("Usuario de enfermería no encontrado."));

        List<String> alerts = VitalSignAlertEvaluator.evaluate(
                form.getSystolic(), form.getDiastolic(), form.getTemperature(), form.getHeartRate());

        VitalSigns vitalSigns = new VitalSigns();
        vitalSigns.setAppointment(appointment);
        vitalSigns.setRecordedBy(nurse);
        vitalSigns.setSystolic(form.getSystolic());
        vitalSigns.setDiastolic(form.getDiastolic());
        vitalSigns.setTemperature(form.getTemperature());
        vitalSigns.setWeight(form.getWeight());
        vitalSigns.setHeight(form.getHeight());
        vitalSigns.setHeartRate(form.getHeartRate());
        vitalSigns.setEmergency(form.isEmergency());
        vitalSigns.setAlerts(alerts.isEmpty() ? null : String.join(" | ", alerts));
        vitalSigns = vitalSignsRepository.save(vitalSigns);

        appointment.setStatus(AppointmentStatus.EN_ESPERA);
        if (form.isEmergency()) {
            appointment.setPriority(AppointmentPriority.EMERGENCIA);
        }
        appointmentRepository.save(appointment);

        return vitalSigns;
    }

    // ------------------------------------------------------------------ auxiliares

    private Appointment load(Integer appointmentId) {
        return appointmentRepository.findByIdWithDetails(appointmentId)
                .orElseThrow(() -> new BusinessRuleException("Cita no encontrada."));
    }

    /** Sucursal del empleado; null si no tiene (entonces ve todas las sucursales). */
    private Integer branchOf(Integer userId) {
        InternalUser user = internalUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("Usuario no encontrado."));
        return user.getBranch() == null ? null : user.getBranch().getId();
    }

    private List<Appointment> queue(List<AppointmentStatus> statuses, LocalDateTime since, Integer branchId) {
        return branchId == null
                ? appointmentRepository.findQueueAll(statuses, since)
                : appointmentRepository.findQueueByBranch(statuses, since, branchId);
    }
}
