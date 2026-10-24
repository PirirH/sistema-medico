package com.hospital.sistemamedico.model.dto;

import com.hospital.sistemamedico.model.enums.AppointmentPriority;
import com.hospital.sistemamedico.model.enums.FollowUpType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/** Formulario de cita de seguimiento (CU-12). */
@Data
public class FollowUpForm {

    @NotNull(message = "Debe seleccionar el tipo de seguimiento.")
    private FollowUpType followUpType;

    @NotNull(message = "Seleccione una fecha futura dentro de los horarios disponibles del médico.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate appointmentDate;

    @NotNull(message = "Seleccione una fecha futura dentro de los horarios disponibles del médico.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime appointmentTime;

    @NotBlank(message = "Las observaciones son obligatorias. Deben contener entre 10 y 2000 caracteres.")
    @Size(min = 10, max = 2000,
            message = "Las observaciones son obligatorias. Deben contener entre 10 y 2000 caracteres.")
    private String reason;

    private AppointmentPriority priority = AppointmentPriority.NORMAL;
}
