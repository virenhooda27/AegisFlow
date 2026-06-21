package com.aegisflow.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record EdgeDto(
        @NotBlank(message = "Source node key is required")
        String sourceNodeKey,

        @NotBlank(message = "Target node key is required")
        String targetNodeKey
) {
}
