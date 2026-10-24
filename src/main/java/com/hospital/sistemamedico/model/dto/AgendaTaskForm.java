package com.hospital.sistemamedico.model.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AgendaTaskForm {
    private String title;
    private String description;
    /** BAJA, NORMAL, ALTA */
    private String priority;
    private LocalDate dueDate;
}
