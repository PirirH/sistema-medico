package com.hospital.sistemamedico.service.impl;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.ReportPoint;
import com.hospital.sistemamedico.model.dto.ReportResult;
import com.hospital.sistemamedico.model.enums.PaymentMethod;
import com.hospital.sistemamedico.repository.ReportRepository;
import com.hospital.sistemamedico.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final int DEFAULT_TOP = 5;
    private static final int MAX_TOP = 20;
    private static final int DEFAULT_DAYS = 90;
    private static final int MAX_RANGE_DAYS = 366 * 3;

    private static final String[] DAY_NAMES = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    private final ReportRepository reportRepository;

    // ------------------------------------------------------------------ 1. Médicos

    @Override
    public ReportResult topDoctors(LocalDate from, LocalDate to, Integer branchId, Integer top) {
        LocalDate[] r = range(from, to);
        List<ReportPoint> points = new ArrayList<>();
        for (Object[] row : reportRepository.topDoctorsByConsultations(
                start(r[0]), endExclusive(r[1]), branchId, safeTop(top))) {
            points.add(new ReportPoint("Dr(a). " + row[1], ((Number) row[3]).longValue(), String.valueOf(row[2])));
        }
        return ReportResult.of("Top de médicos con más consultas", "Consultas finalizadas", "Especialidad", points);
    }

    // ------------------------------------------------------------------ 2. Días de mayor demanda

    @Override
    public ReportResult demandByWeekday(LocalDate from, LocalDate to, Integer branchId) {
        LocalDate[] r = range(from, to);
        Map<DayOfWeek, Long> counts = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) counts.put(d, 0L);
        for (LocalDateTime at : reportRepository.appointmentDates(start(r[0]), endExclusive(r[1]), branchId)) {
            counts.merge(at.getDayOfWeek(), 1L, Long::sum);
        }
        // Siempre los 7 días en orden de calendario (un día sin citas es un dato válido: 0).
        List<ReportPoint> points = new ArrayList<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            points.add(new ReportPoint(DAY_NAMES[d.getValue() - 1], counts.get(d), null));
        }
        return ReportResult.of("Días de mayor demanda", "Citas", null, points);
    }

    // ------------------------------------------------------------------ 3. Medicamentos

    @Override
    public ReportResult topMedicines(LocalDate from, LocalDate to, Integer branchId, Integer top) {
        LocalDate[] r = range(from, to);
        List<ReportPoint> points = new ArrayList<>();
        for (Object[] row : reportRepository.topDispensedMedicines(
                start(r[0]), endExclusive(r[1]), branchId, safeTop(top))) {
            long dispatches = ((Number) row[3]).longValue();
            points.add(new ReportPoint(String.valueOf(row[1]), ((Number) row[2]).longValue(),
                    dispatches + (dispatches == 1 ? " despacho" : " despachos")));
        }
        return ReportResult.of("Medicamentos más despachados", "Unidades despachadas", "Despachos", points);
    }

    // ------------------------------------------------------------------ 4. Métodos de pago

    @Override
    public ReportResult paymentMethods(LocalDate from, LocalDate to, Integer branchId, String source) {
        LocalDate[] r = range(from, to);
        String src = source == null ? "ambos" : source.toLowerCase();
        if (!List.of("citas", "laboratorio", "ambos").contains(src)) {
            throw new BusinessRuleException("La fuente de pagos debe ser: citas, laboratorio o ambos.");
        }

        List<Object[]> rows = new ArrayList<>();
        if (!src.equals("laboratorio")) {
            rows.addAll(reportRepository.appointmentPaymentsByMethod(start(r[0]), endExclusive(r[1]), branchId));
        }
        if (!src.equals("citas")) {
            rows.addAll(reportRepository.labPaymentsByMethod(start(r[0]), endExclusive(r[1]), branchId));
        }

        Map<String, long[]> counts = new LinkedHashMap<>();       // etiqueta -> [cantidad]
        Map<String, BigDecimal> amounts = new LinkedHashMap<>();  // etiqueta -> monto
        for (Object[] row : rows) {
            String label = paymentLabel((PaymentMethod) row[0], (String) row[1]);
            counts.computeIfAbsent(label, k -> new long[1])[0] += ((Number) row[2]).longValue();
            BigDecimal amount = row[3] == null ? BigDecimal.ZERO : (BigDecimal) row[3];
            amounts.merge(label, amount, BigDecimal::add);
        }

        List<ReportPoint> points = new ArrayList<>();
        counts.forEach((label, c) -> points.add(new ReportPoint(label, c[0],
                "Q" + amounts.get(label).setScale(2, RoundingMode.HALF_UP).toPlainString())));
        points.sort(Comparator.comparingLong(ReportPoint::value).reversed()
                .thenComparing(ReportPoint::label));
        return ReportResult.of("Pagos por método de pago", "Pagos aprobados", "Monto total", points);
    }

    /** Efectivo, o la marca de la tarjeta (Visa, Mastercard, Débito…); si no hay marca, el tipo de tarjeta. */
    private static String paymentLabel(PaymentMethod method, String brand) {
        if (method == PaymentMethod.EFECTIVO) return "Efectivo";
        if (brand != null && !brand.isBlank()) return brand.trim();
        return method.getLabel();
    }

    // ------------------------------------------------------------------ utilidades

    /** Rango por defecto: últimos 90 días hasta hoy. Valida el orden y un máximo razonable. */
    private static LocalDate[] range(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate begin = from != null ? from : end.minusDays(DEFAULT_DAYS);
        if (begin.isAfter(end)) {
            throw new BusinessRuleException("La fecha inicial no puede ser posterior a la fecha final.");
        }
        if (begin.plusDays(MAX_RANGE_DAYS).isBefore(end)) {
            throw new BusinessRuleException("El rango de fechas no puede exceder 3 años.");
        }
        return new LocalDate[]{begin, end};
    }

    private static LocalDateTime start(LocalDate d) {
        return d.atStartOfDay();
    }

    private static LocalDateTime endExclusive(LocalDate d) {
        return d.plusDays(1).atStartOfDay();
    }

    private static int safeTop(Integer top) {
        if (top == null) return DEFAULT_TOP;
        return Math.max(1, Math.min(top, MAX_TOP));
    }
}
