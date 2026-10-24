package com.hospital.sistemamedico.scheduler;

import com.hospital.sistemamedico.model.dto.FollowUpEmailData;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.enums.AppointmentStatus;
import com.hospital.sistemamedico.repository.AppointmentRepository;
import com.hospital.sistemamedico.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RN-CU11-05: recordatorio automático de citas de seguimiento (1-2 días antes, no se envía si se canceló).
 * RNF-020: es resiliente a reinicios porque el "ya enviado" vive en la base de datos (reminder_sent),
 * no en memoria. Si el correo falla, la cita sigue pendiente y se reintenta en la siguiente pasada.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FollowUpReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final EmailService emailService;

    // 1 minuto después de arrancar (recupera pendientes tras un reinicio) y luego cada hora.
    @Scheduled(initialDelay = 60_000, fixedDelay = 3_600_000)
    public void sendDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> due = appointmentRepository.findFollowUpsPendingReminder(
                now, now.plusHours(48),
                List.of(AppointmentStatus.CANCELADA, AppointmentStatus.NO_ASISTIO));

        for (Appointment a : due) {
            boolean sent = emailService.sendFollowUpReminder(FollowUpEmailData.builder()
                    .to(a.getPatient().getEmail())
                    .patientName(a.getPatient().getFullName())
                    .appointmentNumber(a.getAppointmentNumber())
                    .doctorName(a.getDoctor().getFullName())
                    .branch(a.getBranchSpecialty().getBranch().getName())
                    .followUpTypeLabel(a.getFollowUpType().getLabel())
                    .observations(a.getReason())
                    .scheduledAt(a.getScheduledAt())
                    .build());
            if (sent) {
                a.setReminderSent(true);
                appointmentRepository.save(a);
            }
        }
        if (!due.isEmpty()) {
            log.info("Recordatorios de seguimiento procesados: {}", due.size());
        }
    }
}
