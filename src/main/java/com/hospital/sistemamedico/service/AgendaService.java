package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.exception.BusinessRuleException;
import com.hospital.sistemamedico.model.dto.AgendaEventForm;
import com.hospital.sistemamedico.model.dto.AgendaTaskForm;
import com.hospital.sistemamedico.model.entity.AgendaEvent;
import com.hospital.sistemamedico.model.entity.AgendaTask;
import com.hospital.sistemamedico.model.entity.Appointment;
import com.hospital.sistemamedico.model.enums.AppointmentStatus;
import com.hospital.sistemamedico.repository.AgendaEventRepository;
import com.hospital.sistemamedico.repository.AgendaTaskRepository;
import com.hospital.sistemamedico.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** CU-16 Gestión de Agenda Médica. */
@Service
@RequiredArgsConstructor
public class AgendaService {

    public static final String EVENT_COLOR = "#8b5cf6";
    private static final Set<String> EVENT_TYPES = Set.of("REUNION", "DESCANSO", "CAPACITACION", "PERSONAL", "OTRO");
    private static final Set<String> PRIORITIES = Set.of("BAJA", "NORMAL", "ALTA");
    private static final List<AppointmentStatus> HIDDEN = List.of(
            AppointmentStatus.PENDIENTE_PAGO, AppointmentStatus.NO_ASISTIO, AppointmentStatus.CANCELADA);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AgendaEventRepository eventRepository;
    private final AgendaTaskRepository taskRepository;
    private final AppointmentRepository appointmentRepository;

    // ---------------------------------------------------------------- calendario

    /** Eventos del médico + sus citas visibles, en el formato que entiende FullCalendar. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> calendar(Integer doctorId, LocalDateTime from, LocalDateTime to) {
        List<Map<String, Object>> out = new ArrayList<>();

        for (AgendaEvent e : eventRepository.findOverlapping(doctorId, from, to)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", "e" + e.getId());
            m.put("title", e.getTitle());
            m.put("allDay", e.isAllDay());
            if (e.isAllDay()) { // FullCalendar usa fin exclusivo para eventos de día completo
                m.put("start", e.getStartAt().toLocalDate().toString());
                m.put("end", e.getEndAt().toLocalDate().plusDays(1).toString());
            } else {
                m.put("start", e.getStartAt().toString());
                m.put("end", e.getEndAt().toString());
            }
            m.put("color", EVENT_COLOR);
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("kind", "event");
            props.put("eventId", e.getId());
            props.put("description", e.getDescription() == null ? "" : e.getDescription());
            props.put("eventType", e.getEventType());
            props.put("startAt", e.getStartAt().toString());
            props.put("endAt", e.getEndAt().toString());
            m.put("extendedProps", props);
            out.add(m);
        }

        for (Appointment a : appointmentRepository.findForAgenda(doctorId, from, to, HIDDEN)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", "a" + a.getId());
            m.put("title", a.getPatient().getFullName());
            m.put("start", a.getScheduledAt().toString());
            m.put("end", a.getScheduledAt().plusMinutes(30).toString());
            m.put("color", statusColor(a.getStatus()));
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("kind", "appointment");
            props.put("patient", a.getPatient().getFullName());
            props.put("status", a.getStatus().getLabel());
            props.put("specialty", a.getBranchSpecialty().getSpecialty().getName());
            props.put("number", a.getAppointmentNumber());
            m.put("extendedProps", props);
            out.add(m);
        }
        return out;
    }

    private static String statusColor(AppointmentStatus s) {
        return switch (s) {
            case CONFIRMADA -> "#2563eb";
            case PACIENTE_PRESENTE, SIGNOS_VITALES, EN_ESPERA -> "#f59e0b";
            case CONSULTA_MEDICA -> "#0d9488";
            case ATENCION_FINALIZADA, COMPLETADA -> "#16a34a";
            default -> "#6b7280";
        };
    }

    // ---------------------------------------------------------------- eventos

    @Transactional
    public String createEvent(Integer doctorId, AgendaEventForm f) {
        validateEvent(f);
        AgendaEvent e = new AgendaEvent();
        e.setDoctorId(doctorId);
        applyEvent(e, f);
        eventRepository.save(e);
        return "Evento creado exitosamente. %s — %s a %s.".formatted(
                e.getTitle(), e.getStartAt().format(DAY), e.getEndAt().format(DAY));
    }

    @Transactional
    public String updateEvent(Integer doctorId, Integer id, AgendaEventForm f) {
        validateEvent(f);
        AgendaEvent e = ownEvent(doctorId, id);
        applyEvent(e, f);
        eventRepository.save(e);
        return "Evento actualizado exitosamente.";
    }

    @Transactional
    public void deleteEvent(Integer doctorId, Integer id) {
        eventRepository.delete(ownEvent(doctorId, id));
    }

    private AgendaEvent ownEvent(Integer doctorId, Integer id) {
        AgendaEvent e = eventRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("El evento solicitado no existe."));
        if (!e.getDoctorId().equals(doctorId)) {
            throw new BusinessRuleException("El evento solicitado no existe.");
        }
        return e;
    }

    private void applyEvent(AgendaEvent e, AgendaEventForm f) {
        e.setTitle(f.getTitle().trim());
        e.setDescription(f.getDescription() == null || f.getDescription().isBlank() ? null : f.getDescription().trim());
        e.setAllDay(f.isAllDay());
        e.setEventType(f.getEventType());
        if (f.isAllDay()) {
            e.setStartAt(f.getStartAt().toLocalDate().atStartOfDay());
            e.setEndAt(f.getEndAt().toLocalDate().atTime(23, 59));
        } else {
            e.setStartAt(f.getStartAt());
            e.setEndAt(f.getEndAt());
        }
    }

    private void validateEvent(AgendaEventForm f) {
        if (f.getTitle() == null || f.getTitle().isBlank()) {
            throw new BusinessRuleException("El título es obligatorio.");
        }
        if (f.getTitle().trim().length() > 150) {
            throw new BusinessRuleException("El título no puede exceder los 150 caracteres.");
        }
        if (f.getStartAt() == null) {
            throw new BusinessRuleException("La fecha de inicio es obligatoria.");
        }
        if (f.getEndAt() == null) {
            throw new BusinessRuleException("La fecha de fin es obligatoria.");
        }
        boolean invalidRange = f.isAllDay()
                ? f.getEndAt().toLocalDate().isBefore(f.getStartAt().toLocalDate())
                : !f.getEndAt().isAfter(f.getStartAt());
        if (invalidRange) {
            throw new BusinessRuleException("La fecha de fin debe ser posterior a la fecha de inicio.");
        }
        if (f.getEventType() == null || !EVENT_TYPES.contains(f.getEventType())) {
            throw new BusinessRuleException("El tipo de evento es obligatorio.");
        }
        if (f.getDescription() != null && f.getDescription().length() > 500) {
            throw new BusinessRuleException("La descripción no puede exceder los 500 caracteres.");
        }
    }

    // ---------------------------------------------------------------- tareas

    @Transactional(readOnly = true)
    public List<Map<String, Object>> tasks(Integer doctorId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (AgendaTask t : taskRepository.findByDoctorIdOrderByDueDateAscIdAsc(doctorId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("description", t.getDescription() == null ? "" : t.getDescription());
            m.put("priority", t.getPriority());
            m.put("dueDate", t.getDueDate().toString());
            m.put("completed", t.isCompleted());
            out.add(m);
        }
        return out;
    }

    @Transactional
    public void createTask(Integer doctorId, AgendaTaskForm f) {
        if (f.getTitle() == null || f.getTitle().isBlank()) {
            throw new BusinessRuleException("El título es obligatorio.");
        }
        if (f.getTitle().trim().length() > 150) {
            throw new BusinessRuleException("El título no puede exceder los 150 caracteres.");
        }
        if (f.getPriority() == null || !PRIORITIES.contains(f.getPriority())) {
            throw new BusinessRuleException("La prioridad es obligatoria.");
        }
        LocalDate due = f.getDueDate();
        if (due == null) {
            throw new BusinessRuleException("La fecha límite es obligatoria.");
        }
        if (f.getDescription() != null && f.getDescription().length() > 1000) {
            throw new BusinessRuleException("La descripción no puede exceder los 1000 caracteres.");
        }
        AgendaTask t = new AgendaTask();
        t.setDoctorId(doctorId);
        t.setTitle(f.getTitle().trim());
        t.setDescription(f.getDescription() == null || f.getDescription().isBlank() ? null : f.getDescription().trim());
        t.setPriority(f.getPriority());
        t.setDueDate(due);
        taskRepository.save(t);
    }

    @Transactional
    public void toggleTask(Integer doctorId, Integer id) {
        AgendaTask t = ownTask(doctorId, id);
        t.setCompleted(!t.isCompleted());
        taskRepository.save(t);
    }

    @Transactional
    public void deleteTask(Integer doctorId, Integer id) {
        taskRepository.delete(ownTask(doctorId, id));
    }

    private AgendaTask ownTask(Integer doctorId, Integer id) {
        AgendaTask t = taskRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("La tarea solicitada no existe."));
        if (!t.getDoctorId().equals(doctorId)) {
            throw new BusinessRuleException("La tarea solicitada no existe.");
        }
        return t;
    }
}
