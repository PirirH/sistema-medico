package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.model.enums.AppointmentStatus;
import com.hospital.sistemamedico.repository.AppointmentRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationHoldService {

    private static final Duration HOLD_DURATION = Duration.ofMinutes(10);

    private final AppointmentRepository appointmentRepository;

    private Cache<Integer, Boolean> holds;

    @PostConstruct
    void init() {
        holds = Caffeine.newBuilder()
                .expireAfterWrite(HOLD_DURATION)
                .scheduler(Scheduler.systemScheduler())
                .evictionListener((Integer appointmentId, Boolean value, RemovalCause cause) ->
                        releaseIfStillPending(appointmentId))
                .build();
    }

    /** Se llama justo cuando se crea la cita en estado PENDIENTE_PAGO (canal PORTAL). */
    public void startHold(Integer appointmentId) {
        holds.put(appointmentId, Boolean.TRUE);
    }

    /** Se llama cuando el pago se aprueba, para cancelar el temporizador antes de que expire. */
    public void confirmHold(Integer appointmentId) {
        holds.invalidate(appointmentId);
    }

    private void releaseIfStillPending(Integer appointmentId) {
        appointmentRepository.findById(appointmentId).ifPresent(appointment -> {
            if (appointment.getStatus() == AppointmentStatus.PENDIENTE_PAGO) {
                appointment.setStatus(AppointmentStatus.CANCELADA);
                appointment.setReservationExpiresAt(null);
                appointmentRepository.save(appointment);
                log.info("Cita {} cancelada automáticamente: venció la reserva de 10 minutos", appointmentId);
            }
        });
    }

    // Red de seguridad: si el servidor se reinicia, el cache en memoria se pierde
    // y podrían quedar citas "huérfanas" en PENDIENTE_PAGO cuyo hold nunca expiró.
    // Esta barrida corre poco seguido (cada 5 min) y usa el índice ya creado en V4
    // solo como respaldo — no es el mecanismo principal.
    @Scheduled(fixedRate = 5 * 60 * 1000)
    void safetyNetSweep() {
        appointmentRepository
                .findByStatusAndReservationExpiresAtBefore(AppointmentStatus.PENDIENTE_PAGO, LocalDateTime.now())
                .forEach(appointment -> {
                    appointment.setStatus(AppointmentStatus.CANCELADA);
                    appointment.setReservationExpiresAt(null);
                    appointmentRepository.save(appointment);
                    log.warn("Cita {} cancelada por barrida de seguridad (posible reinicio del servidor)", appointment.getId());
                });
    }
}