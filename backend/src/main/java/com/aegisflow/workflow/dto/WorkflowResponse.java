package com.aegisflow.workflow.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkflowResponse(
        UUID id,
        String name,
        String description,
        Integer version,
        List<NodeDto> nodes,
        List<EdgeDto> edges,
        Instant createdAt,
        Instant updatedAt
) {
}
