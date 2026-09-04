package com.hospital.sistemamedico.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginForm {

    @NotBlank(message = "El campo Usuario es obligatorio.")
    private String username;

    @NotBlank(message = "El campo Contraseña es obligatorio.")
    private String password;
}