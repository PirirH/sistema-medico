package com.hospital.sistemamedico.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
@Data
@NoArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // RN-GLOBAL-001: obligatorio, 13 dígitos numéricos, único
    @Column(nullable = false, unique = true, length = 13)
    private String dpi;

    // RN-CU02-01: obligatorio, 10-100 caracteres
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    // RN-CU02-02: obligatorio, 8 dígitos
    @Column(nullable = false, length = 8)
    private String phone;

    // RN-CU02-04: obligatorio, formato email válido, único
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    // RN-CU02-03: opcional, 5-50 caracteres
    @Column(name = "insurance_number", length = 50)
    private String insuranceNumber;

    // RN-CU02-05: obligatorio, 8-9 caracteres alfanuméricos, único
    @Column(nullable = false, unique = true, length = 9)
    private String username;

    // RN-CU02-06: obligatorio, mínimo 12 caracteres (almacenado con hash BCrypt)
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean active = true;

    // RN-CU00-03: bloqueo por intentos fallidos (máx 5, bloqueo 15 min)
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}