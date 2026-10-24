package com.hospital.sistemamedico.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Formulario de cita creada por personal de recepción (Walk-in, CU-05 FA04)
 * y de registro de emergencia (CU-05 FA01).
 * Fecha y hora son opcionales aquí porque una emergencia se atiende "ahora":
 * el servicio las exige cuando la cita NO es de emergencia.
 */
@Data
public class WalkInAppointmentForm {

    @NotBlank(message = "Debe ingresar el DPI del paciente.")
    @Pattern(regexp = "\\d{13}", message = "El DPI debe contener exactamente 13 dígitos numéricos.")
    private String patientDpi;

    @NotNull(message = "Debe seleccionar una sucursal.")
    private Integer branchId;

    @NotNull(message = "Debe seleccionar una especialidad.")
    private Integer specialtyId;

    @NotNull(message = "Debe seleccionar un médico.")
    private Integer doctorId;

    private LocalDate appointmentDate;

    private LocalTime appointmentTime;

    @NotBlank(message = "El motivo de la consulta es obligatorio.")
    @Size(min = 10, max = 2000, message = "El motivo de la consulta debe contener entre 10 y 2000 caracteres.")
    private String reason;

    private boolean emergency;
}
