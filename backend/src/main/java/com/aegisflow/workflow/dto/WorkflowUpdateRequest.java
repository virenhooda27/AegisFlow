package com.aegisflow.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record WorkflowUpdateRequest(
        @NotBlank(message = "Workflow name is required")
        @Size(max = 255, message = "Name must be 255 characters or less")
        String name,

        String description,

        @Valid
        List<NodeDto> nodes,

        @Valid
        List<EdgeDto> edges
) {
}
