package com.aegisflow.audit.service;

import com.aegisflow.audit.entity.AuditLog;
import com.aegisflow.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(String entityType, UUID entityId, String action, String actor,
                        Map<String, Object> beforeState, Map<String, Object> afterState) {
        AuditLog log = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .actor(actor)
                .beforeState(beforeState)
                .afterState(afterState)
                .build();
        auditLogRepository.save(log);
    }

    public List<AuditLog> getByEntity(String entityType, UUID entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }

    public List<AuditLog> getRecent() {
        return auditLogRepository.findTop50ByOrderByCreatedAtDesc();
    }
}
