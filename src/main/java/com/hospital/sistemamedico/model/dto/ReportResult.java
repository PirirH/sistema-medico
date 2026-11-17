package com.hospital.sistemamedico.model.dto;

import java.util.List;

/**
 * Respuesta JSON de un reporte. El navegador la entrega tal cual a Chart.js,
 * por eso el tipo de gráfico (barras, pastel…) no se decide aquí sino en la vista.
 *
 * @param title        título del reporte
 * @param metricLabel  qué mide el valor ("Consultas", "Unidades despachadas"…)
 * @param extraLabel   encabezado de la columna complementaria (puede ser null)
 * @param total        suma de los valores de {@code points}
 * @param points       datos ya ordenados
 */
public record ReportResult(String title, String metricLabel, String extraLabel,
                           long total, List<ReportPoint> points) {

    public static ReportResult of(String title, String metricLabel, String extraLabel, List<ReportPoint> points) {
        long total = points.stream().mapToLong(ReportPoint::value).sum();
        return new ReportResult(title, metricLabel, extraLabel, total, points);
    }
}
