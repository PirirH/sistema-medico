package com.hospital.sistemamedico.model.enums;

public enum PrescriptionStatus {
    ACTIVA("Activa"),
    DESPACHADA("Despachada"),
    NO_ADQUIRIDA("No adquirida");

    private final String label;

    PrescriptionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
