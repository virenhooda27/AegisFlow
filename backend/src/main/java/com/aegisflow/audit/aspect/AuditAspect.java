package com.aegisflow.audit.aspect;

import com.aegisflow.audit.service.AuditService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @Around("@annotation(audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        String action = audited.action().isEmpty()
                ? joinPoint.getSignature().getName()
                : audited.action();
        String entityType = audited.entityType();

        log.debug("Audit: {} on {}", action, entityType);

        Object result = joinPoint.proceed();

        // Extract entity ID from result if possible
        UUID entityId = extractEntityId(result);
        if (entityId != null) {
            auditService.record(entityType, entityId, action, "system",
                    null, Map.of("result", result.toString()));
        }

        return result;
    }

    private UUID extractEntityId(Object result) {
        if (result == null) return null;
        try {
            var method = result.getClass().getMethod("getId");
            Object id = method.invoke(result);
            if (id instanceof UUID uuid) return uuid;
        } catch (Exception ignored) {}
        return null;
    }
}
