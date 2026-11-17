package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.model.dto.ReportResult;

import java.time.LocalDate;

public interface ReportService {

    /** Top de médicos con más consultas finalizadas. */
    ReportResult topDoctors(LocalDate from, LocalDate to, Integer branchId, Integer top);

    /** Demanda de citas por día de la semana (lunes a domingo). */
    ReportResult demandByWeekday(LocalDate from, LocalDate to, Integer branchId);

    /** Medicamentos más despachados en farmacia (unidades). */
    ReportResult topMedicines(LocalDate from, LocalDate to, Integer branchId, Integer top);

    /** Pagos aprobados por forma de pago / marca de tarjeta. {@code source}: citas, laboratorio o ambos. */
    ReportResult paymentMethods(LocalDate from, LocalDate to, Integer branchId, String source);
}
