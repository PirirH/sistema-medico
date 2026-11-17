package com.hospital.sistemamedico.model.dto;

/**
 * Un dato de un reporte: la etiqueta que se ve en el gráfico, el valor numérico y un texto
 * complementario opcional (especialidad, monto total, número de despachos…).
 */
public record ReportPoint(String label, long value, String extra) {
}
