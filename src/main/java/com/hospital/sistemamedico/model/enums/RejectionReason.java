package com.hospital.sistemamedico.model.enums;

public enum RejectionReason {
    FONDOS_INSUFICIENTES("Su tarjeta fue rechazada por fondos insuficientes. Verifique su saldo e intente nuevamente."),
    TARJETA_INVALIDA("Su tarjeta fue rechazada. El número de tarjeta es inválido. Verifique los datos e intente nuevamente."),
    TARJETA_VENCIDA("Su tarjeta fue rechazada. La tarjeta está vencida. Utilice otra tarjeta de crédito o débito."),
    ERROR_COMUNICACION("Error al procesar el pago. Por favor, intente nuevamente o contacte a su banco."),
    SESION_EXPIRADA("Su sesión de pago ha expirado. Por favor, inicie el proceso de pago nuevamente.");

    private final String message;

    RejectionReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}