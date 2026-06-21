package com.aegisflow.execution.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TaskRunResponse(
        UUID id,
        String nodeKey,
        String nodeName,
        String nodeType,
        String status,
        Integer attempt,
        Integer maxAttempts,
        Instant startedAt,
        Instant completedAt,
        String errorMessage,
        Map<String, Object> output,
        UUID assignedWorkerId
) {}
