package com.hospital.sistemamedico.service;

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
}