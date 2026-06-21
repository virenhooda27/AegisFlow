package com.aegisflow.execution.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartRunRequest(
        @NotNull(message = "workflowId is required")
        UUID workflowId
) {}
