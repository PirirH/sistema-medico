package com.hospital.sistemamedico.model.enums;

public enum PaymentStatus {
    APROBADO("Aprobado"),
    RECHAZADO("Rechazado");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}