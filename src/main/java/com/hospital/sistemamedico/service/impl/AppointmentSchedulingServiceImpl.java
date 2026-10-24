package com.hospital.sistemamedico.service.impl;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.AppointmentRequestForm;
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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentSchedulingServiceImpl implements AppointmentSchedulingService {

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

        // RN-CU03-01 / RN-CU03-02: la combinación sucursal + especialidad debe existir y estar activa
        BranchSpecialty branchSpecialty = branchSpecialtyRepository
                .findByBranch_IdAndSpecialty_IdAndActiveTrue(form.getBranchId(), form.getSpecialtyId())
                .orElseThrow(() -> new BusinessRuleException(
                        "La especialidad seleccionada no está disponible en la sucursal elegida."));

        InternalUser doctor = validateDoctorAssignment(form.getDoctorId(), form.getBranchId(), form.getSpecialtyId());

        // RN-CU03-05: la fecha y hora deben quedar después del instante actual
        LocalDateTime scheduledAt = LocalDateTime.of(form.getAppointmentDate(), form.getAppointmentTime());
        if (!scheduledAt.isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException(
                    "Debe seleccionar una fecha y hora futuras. Las citas no pueden agendarse en fechas pasadas o presentes.");
        }

        // Revalidamos el horario justo antes de guardar: protege contra dos pacientes
        // reservando el mismo slot casi al mismo tiempo (el paso 4 del wizard pudo
        // haberse cargado minutos antes de que el paciente confirmara).
        List<LocalTime> freeSlots =
                doctorAvailabilityService.getAvailableSlots(doctor.getId(), form.getAppointmentDate());
        if (!freeSlots.contains(form.getAppointmentTime())) {
            throw new BusinessRuleException(
                    "El horario seleccionado ya no está disponible. Por favor, elija otro horario.");
        }

        Appointment appointment = new Appointment();
        appointment.setAppointmentNumber("TMP-" + UUID.randomUUID().toString().substring(0, 15)); // placeholder, se reemplaza abajo
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setBranchSpecialty(branchSpecialty);
        appointment.setScheduledAt(scheduledAt);
        appointment.setDurationMinutes(slotDurationMinutes);
        appointment.setStatus(AppointmentStatus.PENDIENTE_PAGO);
        appointment.setChannel(AppointmentChannel.PORTAL);
        appointment.setPriority(AppointmentPriority.NORMAL);
        appointment.setReason(form.getReason());
        appointment.setTotalAmount(branchSpecialty.getPrice());
        appointment.setReservationExpiresAt(LocalDateTime.now().plusMinutes(10));

        appointment = appointmentRepository.save(appointment);

        // Ya existe el id autogenerado: ahora sí generamos el número de cita legible.
        appointment.setAppointmentNumber(generateAppointmentNumber(appointment.getId()));
        appointment = appointmentRepository.save(appointment);

        reservationHoldService.startHold(appointment.getId());

        return appointment;
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

    private String generateAppointmentNumber(Integer id) {
        return "CIT-" + Year.now().getValue() + "-" + String.format("%06d", id);
    }
}