package com.hospital.sistemamedico.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/** Formulario de captura de signos vitales (CU-07). Los rangos son los de captura (RN-CU07-01 a 05). */
@Data
public class VitalSignsForm {

    public static final String PA_MESSAGE =
            "La presión arterial debe ingresarse en formato sistólica/diastólica (ej: 120/80) dentro de rangos válidos.";

    @NotNull(message = PA_MESSAGE)
    @Min(value = 60, message = PA_MESSAGE)
    @Max(value = 250, message = PA_MESSAGE)
    private Integer systolic;

    @NotNull(message = PA_MESSAGE)
    @Min(value = 40, message = PA_MESSAGE)
    @Max(value = 150, message = PA_MESSAGE)
    private Integer diastolic;

    @NotNull(message = "La temperatura debe estar entre 34.0 y 42.0°C con un decimal.")
    @DecimalMin(value = "34.0", message = "La temperatura debe estar entre 34.0 y 42.0°C con un decimal.")
    @DecimalMax(value = "42.0", message = "La temperatura debe estar entre 34.0 y 42.0°C con un decimal.")
    @Digits(integer = 2, fraction = 1, message = "La temperatura debe estar entre 34.0 y 42.0°C con un decimal.")
    private BigDecimal temperature;

    @NotNull(message = "El peso debe estar entre 0.5 y 300 kg con dos decimales.")
    @DecimalMin(value = "0.5", message = "El peso debe estar entre 0.5 y 300 kg con dos decimales.")
    @DecimalMax(value = "300", message = "El peso debe estar entre 0.5 y 300 kg con dos decimales.")
    @Digits(integer = 3, fraction = 2, message = "El peso debe estar entre 0.5 y 300 kg con dos decimales.")
    private BigDecimal weight;

    @NotNull(message = "La talla debe estar entre 30 y 250 cm con dos decimales.")
    @DecimalMin(value = "30", message = "La talla debe estar entre 30 y 250 cm con dos decimales.")
    @DecimalMax(value = "250", message = "La talla debe estar entre 30 y 250 cm con dos decimales.")
    @Digits(integer = 3, fraction = 2, message = "La talla debe estar entre 30 y 250 cm con dos decimales.")
    private BigDecimal height;

    @NotNull(message = "La frecuencia cardíaca debe estar entre 30 y 220 latidos por minuto.")
    @Min(value = 30, message = "La frecuencia cardíaca debe estar entre 30 y 220 latidos por minuto.")
    @Max(value = 220, message = "La frecuencia cardíaca debe estar entre 30 y 220 latidos por minuto.")
    private Integer heartRate;

    /** Indicador "Es Emergencia" (CU-07 FA01). */
    private boolean emergency;
}
