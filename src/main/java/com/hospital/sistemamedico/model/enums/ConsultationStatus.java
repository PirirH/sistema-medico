package com.hospital.sistemamedico.model.enums;

public enum ConsultationStatus {
    EN_CURSO("En curso"),
    FINALIZADA("Finalizada");

    private final String label;

    ConsultationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
