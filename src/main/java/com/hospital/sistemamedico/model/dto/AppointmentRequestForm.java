package com.hospital.sistemamedico.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Formulario del wizard de Agendar Cita (CU-03, pasos 1 a 4).
 */
@Data
public class AppointmentRequestForm {

    // RN-CU03-02
    @NotNull(message = "Debe seleccionar una sucursal.")
    private Integer branchId;

    // RN-CU03-01
    @NotNull(message = "Debe seleccionar una especialidad.")
    private Integer specialtyId;

    @NotNull(message = "Debe seleccionar un médico.")
    private Integer doctorId;

    // RN-CU03-05
    @NotNull(message = "Debe seleccionar una fecha y hora futuras. Las citas no pueden agendarse en fechas pasadas o presentes.")
    private LocalDate appointmentDate;

    @NotNull(message = "Debe seleccionar una fecha y hora futuras. Las citas no pueden agendarse en fechas pasadas o presentes.")
    private LocalTime appointmentTime;

    // RN-CU03-03
    @NotBlank(message = "El motivo de la consulta es obligatorio.")
    @Size(min = 10, max = 2000, message = "El motivo de la consulta debe contener entre 10 y 2000 caracteres.")
    private String reason;
}