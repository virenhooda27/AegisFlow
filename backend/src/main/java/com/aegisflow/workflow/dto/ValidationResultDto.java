package com.aegisflow.workflow.dto;

import java.util.List;

public record ValidationResultDto(
        boolean valid,
        List<String> errors,
        List<String> warnings
) {
    public static ValidationResultDto success() {
        return new ValidationResultDto(true, List.of(), List.of());
    }

    public static ValidationResultDto failure(List<String> errors) {
        return new ValidationResultDto(false, errors, List.of());
    }

    public static ValidationResultDto withWarnings(List<String> warnings) {
        return new ValidationResultDto(true, List.of(), warnings);
    }
}
