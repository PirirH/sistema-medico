package com.hospital.sistemamedico.model.dto;

import com.hospital.sistemamedico.validation.Dpi;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Formulario de registro de Usuario Externo (CU-02).
 * Cada anotación refleja directamente una regla RN-CU02-01 a RN-CU02-06.
 */
@Data
public class PatientRegistrationForm {

    @Dpi
    private String dpi;

    // RN-CU02-01
    @NotBlank(message = "El campo Nombre es obligatorio.")
    @Size(min = 10, max = 100, message = "El nombre debe contener entre 10 y 100 caracteres.")
    private String fullName;

    // RN-CU02-02
    @NotBlank(message = "El número de teléfono es obligatorio.")
    @Pattern(regexp = "\\d{8}", message = "El número de teléfono debe contener exactamente 8 dígitos numéricos.")
    private String phone;

    // RN-CU02-04
    @NotBlank(message = "El campo Correo Electrónico es obligatorio.")
    @Email(message = "El formato del correo electrónico no es válido. Ejemplo: usuario@dominio.com")
    private String email;

    // RN-CU02-03 (opcional)
    @Size(min = 5, max = 50, message = "El número de seguro debe contener entre 5 y 50 caracteres.")
    private String insuranceNumber;

    // RN-CU02-05
    @NotBlank(message = "El campo Usuario es obligatorio.")
    @Pattern(regexp = "^[a-zA-Z0-9]{8,9}$", message = "El usuario debe contener entre 8 y 9 caracteres alfanuméricos.")
    private String username;

    // RN-CU02-06
    @NotBlank(message = "El campo Contraseña es obligatorio.")
    @Size(min = 12, message = "La contraseña debe contener al menos 12 caracteres.")
    private String password;
}