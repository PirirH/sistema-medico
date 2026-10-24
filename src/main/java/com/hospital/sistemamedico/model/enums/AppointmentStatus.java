package com.hospital.sistemamedico.model.enums;

/**
 * Estados del ciclo de vida de una cita.
 * Se guardan como texto (EnumType.STRING), por lo que agregar valores nuevos NO requiere migración.
 *
 * Flujo normal:
 * PENDIENTE_PAGO -> CONFIRMADA -> PACIENTE_PRESENTE -> SIGNOS_VITALES -> EN_ESPERA
 *   -> CONSULTA_MEDICA -> ATENCION_FINALIZADA
 * Salidas: CANCELADA (reserva vencida / paciente) y NO_ASISTIO (CU-08 FA06).
 * COMPLETADA se conserva por compatibilidad con la Fase 2.
 */
public enum AppointmentStatus {
    PENDIENTE_PAGO("Pendiente de pago"),
    CONFIRMADA("Confirmada"),
    PACIENTE_PRESENTE("Paciente Presente"),
    SIGNOS_VITALES("Signos Vitales"),
    EN_ESPERA("En Espera"),
    CONSULTA_MEDICA("Consulta Médica"),
    ATENCION_FINALIZADA("Atención Finalizada"),
    NO_ASISTIO("No Asistió"),
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
