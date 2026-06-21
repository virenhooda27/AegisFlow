package com.aegisflow.agent.dto;

public record ApprovalActionRequest(
        String resolvedBy,
        String note
) {}
