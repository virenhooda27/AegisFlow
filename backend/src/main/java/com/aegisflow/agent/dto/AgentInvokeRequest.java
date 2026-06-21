package com.aegisflow.agent.dto;

import java.util.Map;
import java.util.UUID;

public record AgentInvokeRequest(
        String agentType,
        String prompt,
        UUID workflowRunId,
        UUID taskRunId,
        Map<String, Object> context
) {}
