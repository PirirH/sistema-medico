package com.hospital.sistemamedico.model.dto;

import com.hospital.sistemamedico.validation.Dpi;
import lombok.Data;

/**
 * Formulario del modal "Verificar Registro" (CU-00, pasos 4-6).
 */
@Data
public class DpiCheckForm {

    @Dpi
    private String dpi;
}