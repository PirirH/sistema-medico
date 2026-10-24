package com.hospital.sistemamedico.model.enums;

/** RN-CU11-01: tipos de seguimiento. */
public enum FollowUpType {
    MONITOREO_TRATAMIENTO("Monitoreo de Tratamiento"),
    REVISION_RESULTADOS("Revisión de Resultados de Laboratorio");

    private final String label;

    FollowUpType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
