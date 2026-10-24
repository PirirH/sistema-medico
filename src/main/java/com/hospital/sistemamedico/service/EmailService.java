package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.model.dto.FollowUpEmailData;
import com.hospital.sistemamedico.model.dto.ReceiptEmailData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    /** RN-GLOBAL-006: teléfono del pie de correo (configurable; valor por defecto de la Sede Central). */
    @Value("${hospital.contact-phone:2234-5678}")
    private String contactPhone;

    @Async
    public void sendPaymentReceipt(ReceiptEmailData data) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(data.getTo());
            message.setSubject("Comprobante de pago - Cita " + data.getAppointmentNumber());
            message.setText("""
                    Hola %s,

                    ¡Pago realizado exitosamente! Su cita ha sido confirmada.

                    Número de transacción: %s
                    Número de cita: %s
                    Médico: Dr(a). %s
                    Especialidad: %s
                    Sucursal: %s
                    Fecha: %s
                    Hora: %s
                    Monto pagado: Q%s

                    Hospital Central
                    """.formatted(
                    data.getPatientName(),
                    data.getTransactionNumber(),
                    data.getAppointmentNumber(),
                    data.getDoctorName(),
                    data.getSpecialty(),
                    data.getBranch(),
                    data.getScheduledAt().format(DATE_FORMAT),
                    data.getScheduledAt().format(TIME_FORMAT),
                    data.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString()));

            mailSender.send(message);
            log.info("Comprobante enviado para la cita {}", data.getAppointmentNumber());
        } catch (Exception ex) {
            // Un fallo de correo nunca debe afectar al pago ya aprobado: solo se registra.
            log.error("No se pudo enviar el comprobante de la cita {}", data.getAppointmentNumber(), ex);
        }
    }

    /** RN-CU11-04. Devuelve true si el correo salió; un fallo nunca lanza excepción. */
    public boolean sendFollowUpNotification(FollowUpEmailData data) {
        return send(data, "Cita de Seguimiento Agendada - Hospital Central",
                "Su cita de seguimiento ha sido agendada.");
    }

    /** RN-CU11-05. Devuelve true si el correo salió (el planificador reintenta si devuelve false). */
    public boolean sendFollowUpReminder(FollowUpEmailData data) {
        return send(data, "Recordatorio: Su Cita de Seguimiento Mañana",
                "Le recordamos su próxima cita de seguimiento.");
    }

    private boolean send(FollowUpEmailData data, String subject, String intro) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(data.getTo());
            message.setSubject(subject);
            message.setText("""
                    Hola %s,

                    %s

                    Número de cita: %s
                    Tipo de seguimiento: %s
                    Médico: Dr(a). %s
                    Sucursal: %s
                    Fecha: %s
                    Hora: %s
                    Observaciones: %s

                    Este es un correo automático del Sistema Informático Hospitalario. No responda a este mensaje. Para consultas, comuníquese al teléfono %s.
                    """.formatted(
                    data.getPatientName(),
                    intro,
                    data.getAppointmentNumber(),
                    data.getFollowUpTypeLabel(),
                    data.getDoctorName(),
                    data.getBranch(),
                    data.getScheduledAt().format(DATE_FORMAT),
                    data.getScheduledAt().format(TIME_FORMAT),
                    data.getObservations() == null ? "-" : data.getObservations(),
                    contactPhone));
            mailSender.send(message);
            log.info("Correo '{}' enviado para la cita {}", subject, data.getAppointmentNumber());
            return true;
        } catch (Exception ex) {
            // RN-GLOBAL-006
            log.error("Error al enviar notificación por correo electrónico al paciente {}. Se reintentará automáticamente.",
                    data.getPatientName(), ex);
            return false;
        }
    }
}
