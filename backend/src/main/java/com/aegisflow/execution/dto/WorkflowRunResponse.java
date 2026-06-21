package com.aegisflow.execution.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkflowRunResponse(
        UUID id,
        UUID workflowId,
        String workflowName,
        Integer workflowVersion,
        String status,
        Instant startedAt,
        Instant completedAt,
        String errorMessage,
        List<TaskRunResponse> taskRuns,
        Instant createdAt
) {}
