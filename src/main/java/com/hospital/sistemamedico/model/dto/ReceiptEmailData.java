package com.hospital.sistemamedico.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Datos del comprobante de pago para el correo (RN-CU04-05).
 * Se arma con valores simples (no entidades) porque el correo se envía en otro hilo,
 * donde las relaciones LAZY de Hibernate ya no se pueden cargar.
 */
@Data
@Builder
public class ReceiptEmailData {
    private String to;
    private String patientName;
    private String appointmentNumber;
    private String transactionNumber;
    private String doctorName;
    private String specialty;
    private String branch;
    private LocalDateTime scheduledAt;
    private BigDecimal amount;
}