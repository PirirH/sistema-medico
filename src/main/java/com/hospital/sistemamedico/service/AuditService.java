package com.hospital.sistemamedico.service;

import com.hospital.sistemamedico.model.entity.AuditLog;
import com.hospital.sistemamedico.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(String action, String entity, Integer entityId, String detail, Integer performedBy) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntity(entity);
        log.setEntityId(entityId);
        log.setDetail(detail != null && detail.length() > 500 ? detail.substring(0, 500) : detail);
        log.setPerformedBy(performedBy);
        auditLogRepository.save(log);
    }
}
