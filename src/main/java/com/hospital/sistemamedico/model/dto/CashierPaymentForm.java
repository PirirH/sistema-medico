package com.hospital.sistemamedico.model.dto;

import com.hospital.sistemamedico.model.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Formulario de cobro en caja (CU-06).
 */
@Data
public class CashierPaymentForm {

    @NotNull(message = "Debe seleccionar una forma de pago.")
    private PaymentMethod method;

    // Solo aplica si method == EFECTIVO
    private BigDecimal amountReceived;

    // Solo aplica si method == TARJETA_CREDITO o TARJETA_DEBITO
    private String cardBrand;      // "Visa" o "Mastercard", elegido por el cajero
    private String cardLastFour;   // 4 dígitos, solo como referencia
}