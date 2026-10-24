package com.hospital.sistemamedico.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Datos del correo de seguimiento (valores simples, sin entidades lazy). RN-CU11-04 / RN-CU11-05. */
@Data
@Builder
public class FollowUpEmailData {
    private String to;
    private String patientName;
    private String appointmentNumber;
    private String doctorName;
    private String branch;
    private String followUpTypeLabel;
    private String observations;
    private LocalDateTime scheduledAt;
}
