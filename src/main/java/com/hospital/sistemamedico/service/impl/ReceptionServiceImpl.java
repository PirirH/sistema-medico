package com.hospital.sistemamedico.service.impl;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.ReceptionSearchResult;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.entity.InternalUser;
import com.hospital.sistemamedico.model.enums.AppointmentStatus;
import com.hospital.sistemamedico.repository.AppointmentRepository;
import com.hospital.sistemamedico.repository.InternalUserRepository;
import com.hospital.sistemamedico.repository.PatientRepository;
import com.hospital.sistemamedico.service.ReceptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceptionServiceImpl implements ReceptionService {

    private static final List<AppointmentStatus> ACTIVE_STATUSES = List.of(
            AppointmentStatus.PENDIENTE_PAGO,
            AppointmentStatus.CONFIRMADA,
            AppointmentStatus.PACIENTE_PRESENTE);

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final InternalUserRepository internalUserRepository;

    @Override
    public ReceptionSearchResult search(String appointmentNumber, String dpi) {
        boolean hasNumber = appointmentNumber != null && !appointmentNumber.isBlank();
        boolean hasDpi = dpi != null && !dpi.isBlank();

        if (!hasNumber && !hasDpi) {
            throw new BusinessRuleException("Debe ingresar un número de cita o DPI para buscar.");
        }

        if (hasNumber) {
            Appointment appointment = appointmentRepository.findByAppointmentNumber(appointmentNumber.trim())
                    .orElseThrow(() -> new BusinessRuleException(
                            "No se encontró una cita asociada a los parámetros ingresados. Verifique los datos e intente nuevamente."));
            return new ReceptionSearchResult(ReceptionSearchResult.Outcome.CITA_ENCONTRADA, appointment);
        }

        // Búsqueda por DPI (FA02): primero la cita activa más próxima, si no hay,
        // distinguimos entre "no existe el paciente" y "existe pero sin citas" (FA03/FA04).
        List<Appointment> activeAppointments = appointmentRepository
                .findByPatient_DpiAndStatusInOrderByScheduledAtAsc(dpi.trim(), ACTIVE_STATUSES);

        if (!activeAppointments.isEmpty()) {
            return new ReceptionSearchResult(ReceptionSearchResult.Outcome.CITA_ENCONTRADA, activeAppointments.get(0));
        }

        if (!patientRepository.existsByDpi(dpi.trim())) {
            return new ReceptionSearchResult(ReceptionSearchResult.Outcome.PACIENTE_NO_REGISTRADO, null);
        }

        return new ReceptionSearchResult(ReceptionSearchResult.Outcome.PACIENTE_SIN_CITAS_ACTIVAS, null);
    }

    @Override
    @Transactional
    public Appointment registerArrival(Integer appointmentId, Long expectedVersion) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessRuleException("Cita no encontrada."));

        if (appointment.getStatus() == AppointmentStatus.PENDIENTE_PAGO) {
            throw new BusinessRuleException(
                    "La cita del paciente tiene estado 'Pendiente de pago'. Debe realizar el pago en caja antes de ser atendido.");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELADA) {
            throw new BusinessRuleException("La cita fue cancelada. El paciente debe agendar una nueva cita.");
        }
        if (appointment.getStatus() != AppointmentStatus.CONFIRMADA) {
            throw new BusinessRuleException("Esta cita no puede registrar llegada en su estado actual.");
        }

        // FA09: si la versión que el recepcionista tenía en pantalla ya no coincide
        // con la versión actual en base de datos, alguien más modificó la cita mientras
        // tanto (ej. otro recepcionista, o el cajero confirmó el pago justo ahora).
        if (!appointment.getVersion().equals(expectedVersion)) {
            throw new BusinessRuleException(
                    "Operación no permitida: la cita ya fue actualizada por otro usuario. Verifique el estado actual e intente nuevamente.");
        }

        appointment.setStatus(AppointmentStatus.PACIENTE_PRESENTE);
        appointment.setArrivalTime(LocalDateTime.now());

        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public Appointment reassignDoctor(Integer appointmentId, Integer newDoctorId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessRuleException("Cita no encontrada."));

        if (appointment.getStatus() != AppointmentStatus.CONFIRMADA
                && appointment.getStatus() != AppointmentStatus.PACIENTE_PRESENTE) {
            throw new BusinessRuleException(
                    "Solo se puede reasignar médico en citas con estado Confirmada o Paciente Presente.");
        }

        Integer branchId = appointment.getBranchSpecialty().getBranch().getId();
        Integer specialtyId = appointment.getBranchSpecialty().getSpecialty().getId();

        InternalUser newDoctor = internalUserRepository
                .findByRole_NameAndBranch_IdAndSpecialty_IdAndActiveTrue("Medico", branchId, specialtyId)
                .stream()
                .filter(d -> d.getId().equals(newDoctorId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "El médico seleccionado no pertenece a esa sucursal y especialidad."));

        appointment.setDoctor(newDoctor);
        return appointmentRepository.save(appointment);
    }
}