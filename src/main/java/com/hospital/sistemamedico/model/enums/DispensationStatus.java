package com.hospital.sistemamedico.model.enums;

public enum DispensationStatus {
    DESPACHADO("Despachado"),
    NO_ADQUIRIDO("No adquirido");

    private final String label;

    DispensationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
