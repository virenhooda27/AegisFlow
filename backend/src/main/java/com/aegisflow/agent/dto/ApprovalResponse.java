package com.aegisflow.agent.dto;

import java.time.Instant;
import java.util.UUID;

public record ApprovalResponse(
        UUID id,
        UUID workflowRunId,
        UUID taskRunId,
        String title,
        String description,
        String status,
        Instant requestedAt,
        Instant resolvedAt,
        String resolvedBy,
        String resolutionNote
) {}
