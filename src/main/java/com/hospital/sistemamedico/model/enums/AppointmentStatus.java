package com.hospital.sistemamedico.model.enums;

public enum AppointmentStatus {
    PENDIENTE_PAGO("Pendiente de pago"),
    CONFIRMADA("Confirmada"),
    PACIENTE_PRESENTE("Paciente Presente"),
    CANCELADA("Cancelada"),
    COMPLETADA("Completada");

    private final String label;

    AppointmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}