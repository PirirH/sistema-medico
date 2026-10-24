package com.hospital.sistemamedico.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Registro de auditoría: acción, quién la ejecutó y cuándo (postcondición de CU-01, CU-13, CU-14). */
@Entity
@Table(name = "audit_log")
@Data
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 50)
    private String entity;

    @Column(name = "entity_id")
    private Integer entityId;

    @Column(length = 500)
    private String detail;

    @Column(name = "performed_by")
    private Integer performedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
