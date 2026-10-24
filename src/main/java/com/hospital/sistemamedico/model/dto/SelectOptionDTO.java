package com.hospital.sistemamedico.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO genérico para poblar listas desplegables (<select>) vía JSON,
 * sin exponer la entidad completa (ej. InternalUser trae el hash de password).
 */
@Data
@AllArgsConstructor
public class SelectOptionDTO {
    private Integer id;
    private String label;
}