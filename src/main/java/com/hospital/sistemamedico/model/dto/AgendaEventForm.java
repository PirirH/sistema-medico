package com.hospital.sistemamedico.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgendaEventForm {
    private String title;
    private String description;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    /** REUNION, DESCANSO, CAPACITACION, PERSONAL, OTRO */
    private String eventType;
    private boolean allDay;
}
