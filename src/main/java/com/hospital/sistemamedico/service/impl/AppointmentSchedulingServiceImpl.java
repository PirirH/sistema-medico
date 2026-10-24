package com.hospital.sistemamedico.service.impl;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.AppointmentRequestForm;
import com.hospital.sistemamedico.model.dto.WalkInAppointmentForm;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.entity.BranchSpecialty;
import com.hospital.sistemamedico.model.entity.InternalUser;
import com.hospital.sistemamedico.model.entity.Patient;
import com.hospital.sistemamedico.model.enums.AppointmentChannel;
import com.hospital.sistemamedico.model.enums.AppointmentPriority;
import com.hospital.sistemamedico.model.enums.AppointmentStatus;
import com.hospital.sistemamedico.repository.AppointmentRepository;
import com.hospital.sistemamedico.repository.BranchSpecialtyRepository;
import com.hospital.sistemamedico.repository.InternalUserRepository;
import com.hospital.sistemamedico.repository.PatientRepository;
import com.hospital.sistemamedico.service.AppointmentSchedulingService;
import com.hospital.sistemamedico.service.DoctorAvailabilityService;
import com.hospital.sistemamedico.service.ReservationHoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentSchedulingServiceImpl implements AppointmentSchedulingService {

    private static final String PAST_DATE_MESSAGE =
            "Debe seleccionar una fecha y hora futuras. Las citas no pueden agendarse en fechas pasadas o presentes.";

    private static final int PORTAL_RESERVATION_MINUTES = 10;

    private final PatientRepository patientRepository;
    private final InternalUserRepository internalUserRepository;
    private final BranchSpecialtyRepository branchSpecialtyRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorAvailabilityService doctorAvailabilityService;
    private final ReservationHoldService reservationHoldService;

    @Value("${hospital.schedule.slot-duration-minutes}")
    private int slotDurationMinutes;

    @Override
    @Transactional
    public Appointment scheduleFromPortal(Integer patientId, AppointmentRequestForm form) {

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new BusinessRuleException("Paciente no encontrado."));

        BranchSpecialty branchSpecialty = loadBranchSpecialty(form.getBranchId(), form.getSpecialtyId());
        InternalUser doctor = validateDoctorAssignment(form.getDoctorId(), form.getBranchId(), form.getSpecialtyId());
        LocalDateTime scheduledAt = validateFutureFreeSlot(doctor, form.getAppointmentDate(), form.getAppointmentTime());

        Appointment appointment = buildAppointment(patient, doctor, branchSpecialty, scheduledAt, form.getReason(),
                AppointmentChannel.PORTAL, AppointmentPriority.NORMAL, AppointmentStatus.PENDIENTE_PAGO);
        appointment.setReservationExpiresAt(LocalDateTime.now().plusMinutes(PORTAL_RESERVATION_MINUTES));

        appointment = persistWithNumber(appointment);
        reservationHoldService.startHold(appointment.getId());

        return appointment;
    }

    @Override
    @Transactional
    public Appointment scheduleWalkIn(WalkInAppointmentForm form) {

        Patient patient = patientRepository.findByDpi(form.getPatientDpi().trim())
                .orElseThrow(() -> new BusinessRuleException("No se encontró ningún paciente con ese DPI."));

        BranchSpecialty branchSpecialty = loadBranchSpecialty(form.getBranchId(), form.getSpecialtyId());
        InternalUser doctor = validateDoctorAssignment(form.getDoctorId(), form.getBranchId(), form.getSpecialtyId());

        if (form.isEmergency()) {
            // CU-05 FA01: la emergencia se registra "ahora" y pasa directo a signos vitales,
            // sin esperar un horario libre ni un pago previo.
            LocalDateTime now = LocalDateTime.now();
            Appointment emergency = buildAppointment(patient, doctor, branchSpecialty,
                    now.withSecond(0).withNano(0), form.getReason(),
                    AppointmentChannel.PRESENCIAL, AppointmentPriority.EMERGENCIA, AppointmentStatus.PACIENTE_PRESENTE);
            emergency.setArrivalTime(now);
            return persistWithNumber(emergency);
        }

        LocalDateTime scheduledAt = validateFutureFreeSlot(doctor, form.getAppointmentDate(), form.getAppointmentTime());

        // Las citas creadas por personal interno no se cancelan automáticamente:
        // no llevan reservationExpiresAt ni hold.
        Appointment appointment = buildAppointment(patient, doctor, branchSpecialty, scheduledAt, form.getReason(),
                AppointmentChannel.PRESENCIAL, AppointmentPriority.NORMAL, AppointmentStatus.PENDIENTE_PAGO);
        return persistWithNumber(appointment);
    }

    private BranchSpecialty loadBranchSpecialty(Integer branchId, Integer specialtyId) {
        return branchSpecialtyRepository
                .findByBranch_IdAndSpecialty_IdAndActiveTrue(branchId, specialtyId)
                .orElseThrow(() -> new BusinessRuleException(
                        "La especialidad seleccionada no está disponible en la sucursal elegida."));
    }

    private InternalUser validateDoctorAssignment(Integer doctorId, Integer branchId, Integer specialtyId) {
        List<InternalUser> validDoctors = internalUserRepository
                .findByRole_NameAndBranch_IdAndSpecialty_IdAndActiveTrue("Medico", branchId, specialtyId);

        return validDoctors.stream()
                .filter(d -> d.getId().equals(doctorId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "El médico seleccionado no está disponible para esa sucursal y especialidad."));
    }

    /** Valida RN-CU03-05 (fecha y hora futuras) y que el horario siga libre justo antes de guardar. */
    private LocalDateTime validateFutureFreeSlot(InternalUser doctor, LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            throw new BusinessRuleException(PAST_DATE_MESSAGE);
        }

        LocalDateTime scheduledAt = LocalDateTime.of(date, time);
        if (!scheduledAt.isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException(PAST_DATE_MESSAGE);
        }

        List<LocalTime> freeSlots = doctorAvailabilityService.getAvailableSlots(doctor.getId(), date);
        if (!freeSlots.contains(time)) {
            throw new BusinessRuleException(
                    "El horario seleccionado ya no está disponible. Por favor, elija otro horario.");
        }
        return scheduledAt;
    }

    private Appointment buildAppointment(Patient patient, InternalUser doctor, BranchSpecialty branchSpecialty,
                                         LocalDateTime scheduledAt, String reason,
                                         AppointmentChannel channel, AppointmentPriority priority,
                                         AppointmentStatus status) {
        Appointment appointment = new Appointment();
        // Placeholder único de 19 caracteres (la columna admite 20); se reemplaza al conocer el id.
        appointment.setAppointmentNumber("TMP-" + UUID.randomUUID().toString().substring(0, 15));
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setBranchSpecialty(branchSpecialty);
        appointment.setScheduledAt(scheduledAt);
        appointment.setDurationMinutes(slotDurationMinutes);
        appointment.setStatus(status);
        appointment.setChannel(channel);
        appointment.setPriority(priority);
        appointment.setReason(reason);
        appointment.setTotalAmount(branchSpecialty.getPrice());
        return appointment;
    }

    /** Guardado en dos fases: el número de cita legible depende del id autogenerado. */
    private Appointment persistWithNumber(Appointment appointment) {
        Appointment saved = appointmentRepository.save(appointment);
        saved.setAppointmentNumber("CIT-" + Year.now().getValue() + "-" + String.format("%06d", saved.getId()));
        return appointmentRepository.save(saved);
    }
}
