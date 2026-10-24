package com.hospital.sistemamedico.repository;

import com.hospital.sistemamedico.model.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {
}
