package com.hospital.sistemamedico.controller;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.AgendaEventForm;
import com.hospital.sistemamedico.model.dto.AgendaTaskForm;
import com.hospital.sistemamedico.security.AppUserPrincipal;
import com.hospital.sistemamedico.service.AgendaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** CU-16 Gestión de Agenda Médica: página + API JSON (la usa FullCalendar). */
@Controller
@RequestMapping("/admin/agenda")
@RequiredArgsConstructor
public class AgendaController {

    private final AgendaService agendaService;

    @GetMapping
    public String page() {
        return "agenda/agenda";
    }

    @GetMapping("/api/calendario")
    @ResponseBody
    public List<Map<String, Object>> calendar(@RequestParam String start, @RequestParam String end,
                                              @AuthenticationPrincipal AppUserPrincipal principal) {
        return agendaService.calendar(principal.getId(), parse(start), parse(end));
    }

    @PostMapping("/api/eventos")
    @ResponseBody
    public ResponseEntity<Map<String, String>> createEvent(@RequestBody AgendaEventForm form,
                                                           @AuthenticationPrincipal AppUserPrincipal principal) {
        return ok(() -> agendaService.createEvent(principal.getId(), form));
    }

    @PutMapping("/api/eventos/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updateEvent(@PathVariable Integer id, @RequestBody AgendaEventForm form,
                                                           @AuthenticationPrincipal AppUserPrincipal principal) {
        return ok(() -> agendaService.updateEvent(principal.getId(), id, form));
    }

    @DeleteMapping("/api/eventos/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteEvent(@PathVariable Integer id,
                                                           @AuthenticationPrincipal AppUserPrincipal principal) {
        return ok(() -> { agendaService.deleteEvent(principal.getId(), id); return "Evento eliminado."; });
    }

    @GetMapping("/api/tareas")
    @ResponseBody
    public List<Map<String, Object>> tasks(@AuthenticationPrincipal AppUserPrincipal principal) {
        return agendaService.tasks(principal.getId());
    }

    @PostMapping("/api/tareas")
    @ResponseBody
    public ResponseEntity<Map<String, String>> createTask(@RequestBody AgendaTaskForm form,
                                                          @AuthenticationPrincipal AppUserPrincipal principal) {
        return ok(() -> { agendaService.createTask(principal.getId(), form); return "Tarea creada."; });
    }

    @PostMapping("/api/tareas/{id}/completar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> toggleTask(@PathVariable Integer id,
                                                          @AuthenticationPrincipal AppUserPrincipal principal) {
        return ok(() -> { agendaService.toggleTask(principal.getId(), id); return "Tarea actualizada."; });
    }

    @DeleteMapping("/api/tareas/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteTask(@PathVariable Integer id,
                                                          @AuthenticationPrincipal AppUserPrincipal principal) {
        return ok(() -> { agendaService.deleteTask(principal.getId(), id); return "Tarea eliminada."; });
    }

    // ---- auxiliares

    private ResponseEntity<Map<String, String>> ok(java.util.function.Supplier<String> action) {
        try {
            return ResponseEntity.ok(Map.of("message", action.get()));
        } catch (BusinessRuleException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    /** FullCalendar envía ISO con zona ("2026-10-01T00:00:00-06:00"); se toma la hora local escrita. */
    private static LocalDateTime parse(String iso) {
        try {
            return OffsetDateTime.parse(iso).toLocalDateTime();
        } catch (Exception ex) {
            return LocalDateTime.parse(iso.length() > 19 ? iso.substring(0, 19) : iso);
        }
    }
}
