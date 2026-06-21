package com.aegisflow.agent.dto;

import java.util.Map;

public record AgentResponse(
        String agentType,
        String result,
        Map<String, Object> structuredOutput,
        int tokensUsed,
        long executionTimeMs
) {}
