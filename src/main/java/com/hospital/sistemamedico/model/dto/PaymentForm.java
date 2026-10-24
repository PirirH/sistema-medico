package com.hospital.sistemamedico.model.dto;

import com.hospital.sistemamedico.validation.CardExpiry;
import com.hospital.sistemamedico.validation.CardNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Formulario de pago con tarjeta (CU-04, paso 2).
 */
@Data
public class PaymentForm {

    // RN-CU04-01
    @CardNumber
    private String cardNumber;

    // RN-CU04-02
    @NotBlank(message = "El nombre del titular es obligatorio.")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ ]{5,100}$",
            message = "El nombre del titular debe contener entre 5 y 100 caracteres alfabéticos sin especiales.")
    private String cardHolderName;

    // RN-CU04-03
    @CardExpiry
    private String expiryDate; // formato MM/AA

    // RN-CU04-04
    @NotBlank(message = "El CVV es obligatorio.")
    @Pattern(regexp = "\\d{3,4}", message = "El CVV debe contener 3 ó 4 dígitos numéricos.")
    private String cvv;
}