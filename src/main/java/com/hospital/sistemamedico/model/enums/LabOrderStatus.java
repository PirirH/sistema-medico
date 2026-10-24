package com.hospital.sistemamedico.model.enums;

/** Estados de una orden de laboratorio (CU-09 / CU-10). */
public enum LabOrderStatus {
    PENDIENTE("Pendiente"),
    EN_PROCESO("En proceso"),
    COMPLETADA("Completada");

    private final String label;

    LabOrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
