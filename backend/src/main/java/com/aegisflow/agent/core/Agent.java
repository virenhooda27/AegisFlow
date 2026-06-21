package com.aegisflow.agent.core;

import java.util.Map;

public interface Agent {

    record AgentInput(
            String prompt,
            Map<String, Object> context,
            Map<String, Object> parameters
    ) {
        public AgentInput(String prompt) {
            this(prompt, Map.of(), Map.of());
        }
    }

    record AgentOutput(
            String result,
            Map<String, Object> structuredOutput,
            int tokensUsed,
            long executionTimeMs
    ) {}

    AgentOutput execute(AgentInput input);

    String agentType();
}
