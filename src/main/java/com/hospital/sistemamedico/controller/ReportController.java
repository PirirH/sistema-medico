package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.ReportResult;
import com.hospital.sistemamedico.repository.BranchRepository;
import com.hospital.sistemamedico.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Módulo de reportes (solo ADMINISTRADOR, ver SecurityConfig). La página se sirve con Thymeleaf y
 * los datos de cada gráfico se piden por separado a los endpoints /api/*, que devuelven JSON.
 */
@Controller
@RequestMapping("/admin/reportes")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final BranchRepository branchRepository;

    @GetMapping
    public String page(Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute("branches", branchRepository.findByActiveTrue());
        model.addAttribute("defaultFrom", today.minusDays(90));
        model.addAttribute("defaultTo", today);
        return "admin/reportes";
    }

    @GetMapping("/api/medicos")
    @ResponseBody
    public ResponseEntity<?> doctors(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                     @RequestParam(required = false) Integer sucursalId,
                                     @RequestParam(required = false) Integer top) {
        return respond(() -> reportService.topDoctors(desde, hasta, sucursalId, top));
    }

    @GetMapping("/api/dias")
    @ResponseBody
    public ResponseEntity<?> weekdays(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                      @RequestParam(required = false) Integer sucursalId) {
        return respond(() -> reportService.demandByWeekday(desde, hasta, sucursalId));
    }

    @GetMapping("/api/medicamentos")
    @ResponseBody
    public ResponseEntity<?> medicines(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                       @RequestParam(required = false) Integer sucursalId,
                                       @RequestParam(required = false) Integer top) {
        return respond(() -> reportService.topMedicines(desde, hasta, sucursalId, top));
    }

    @GetMapping("/api/pagos")
    @ResponseBody
    public ResponseEntity<?> payments(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                      @RequestParam(required = false) Integer sucursalId,
                                      @RequestParam(required = false) String fuente) {
        return respond(() -> reportService.paymentMethods(desde, hasta, sucursalId, fuente));
    }

    /** Una regla de negocio incumplida (fechas invertidas…) se devuelve como 400 con el mensaje para mostrarlo en pantalla. */
    private ResponseEntity<?> respond(Supplier<ReportResult> report) {
        try {
            return ResponseEntity.ok(report.get());
        } catch (BusinessRuleException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
