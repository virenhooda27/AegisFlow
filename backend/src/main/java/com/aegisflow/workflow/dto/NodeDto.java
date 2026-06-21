package com.aegisflow.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record NodeDto(
        UUID id,

        @NotBlank(message = "Node key is required")
        String nodeKey,

        @NotBlank(message = "Node name is required")
        String name,

        @NotBlank(message = "Node type is required")
        String type,

        Map<String, Object> config,

        Integer timeoutSeconds,

        Map<String, Object> retryPolicy,

        @NotNull(message = "Position X is required")
        Double positionX,

        @NotNull(message = "Position Y is required")
        Double positionY
) {
}
