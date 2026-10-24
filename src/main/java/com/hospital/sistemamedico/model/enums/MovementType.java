package com.hospital.sistemamedico.model.enums;

/**
 * Tipos de movimiento de inventario (RN-CU13-01). El código numérico es el del caso de uso;
 * en la base de datos se guarda el nombre del enum (texto).
 * DESPACHO (6) es automático: lo genera el módulo de despacho y NO aparece en el formulario manual.
 */
public enum MovementType {
    COMPRA(0, "Compra", true, "Factura"),
    DEVOLUCION(1, "Devolución", true, "Devolución"),
    VENTA(2, "Venta", false, "Venta"),
    RECLAMO(3, "Reclamo", false, "Reclamo"),
    AJUSTE_POSITIVO(4, "Ajuste+", true, "Referencia"),
    AJUSTE_NEGATIVO(5, "Ajuste-", false, "Referencia"),
    DESPACHO(6, "Despacho", false, "Despacho");

    private final int code;
    private final String label;
    private final boolean entry;
    private final String referenceLabel;

    MovementType(int code, String label, boolean entry, String referenceLabel) {
        this.code = code;
        this.label = label;
        this.entry = entry;
        this.referenceLabel = referenceLabel;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /** true = suma al stock; false = resta. */
    public boolean isEntry() {
        return entry;
    }

    public String getReferenceLabel() {
        return referenceLabel;
    }

    /** Se puede crear a mano desde el formulario (todos menos DESPACHO). */
    public boolean isManual() {
        return this != DESPACHO;
    }

    /** El motivo/notas es obligatorio en Ajuste+, Ajuste-, Devolución y Reclamo. */
    public boolean isNotesRequired() {
        return this == AJUSTE_POSITIVO || this == AJUSTE_NEGATIVO || this == DEVOLUCION || this == RECLAMO;
    }
}
