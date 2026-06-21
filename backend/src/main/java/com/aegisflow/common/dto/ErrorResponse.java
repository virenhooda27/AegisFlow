package com.aegisflow.common.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        int status,
        String message,
        List<String> errors,
        String path,
        Instant timestamp
) {
    public ErrorResponse(int status, String message, String path) {
        this(status, message, List.of(), path, Instant.now());
    }

    public ErrorResponse(int status, String message, List<String> errors, String path) {
        this(status, message, errors, path, Instant.now());
    }
}
