package com.aegisflow.audit.controller;

import com.aegisflow.audit.entity.AuditLog;
import com.aegisflow.audit.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<AuditLog>> getRecent() {
        return ResponseEntity.ok(auditService.getRecent());
    }

    @GetMapping("/{entityType}/{entityId}")
    public ResponseEntity<List<AuditLog>> getByEntity(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        return ResponseEntity.ok(auditService.getByEntity(entityType, entityId));
    }
}
