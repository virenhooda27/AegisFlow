package com.aegisflow.agent.agents;

import com.aegisflow.agent.core.Agent;
import com.aegisflow.agent.llm.LLMProvider;
import com.aegisflow.agent.llm.LLMProviderRegistry;
import com.aegisflow.agent.service.AgentMemoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class RecoveryAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(RecoveryAgent.class);
    private static final String SYSTEM_PROMPT = """
            You are a Recovery Agent for a workflow orchestration platform.
            Your role is to analyze task failures and provide structured recovery plans.
            
            Given failure context, you must respond with JSON containing:
            - rootCause: one of NETWORK, TIMEOUT, DEPENDENCY, CONFIG, DATA, CODE, PERMISSION, UNKNOWN
            - confidence: 0.0 to 1.0
            - analysis: detailed explanation of the failure
            - recoveryPlan: array of ordered steps to recover
            
            Respond ONLY with valid JSON, no markdown formatting.
            """;

    private final LLMProviderRegistry providerRegistry;
    private final AgentMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public RecoveryAgent(LLMProviderRegistry providerRegistry,
                          AgentMemoryService memoryService,
                          ObjectMapper objectMapper) {
        this.providerRegistry = providerRegistry;
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentOutput execute(AgentInput input) {
        long startMs = System.currentTimeMillis();
        log.info("RecoveryAgent analyzing failure...");

        String prompt = buildPrompt(input);
        LLMProvider provider = providerRegistry.getDefault();
        LLMProvider.LLMResponse response = provider.complete(
                new LLMProvider.LLMRequest(prompt, SYSTEM_PROMPT, 0.3, 2048, Map.of()));

        long elapsed = System.currentTimeMillis() - startMs;

        // Store memory
        UUID runId = input.context().containsKey("workflowRunId")
                ? UUID.fromString(input.context().get("workflowRunId").toString()) : null;
        UUID taskId = input.context().containsKey("taskRunId")
                ? UUID.fromString(input.context().get("taskRunId").toString()) : null;

        memoryService.store(runId, taskId, agentType(), "PROMPT", prompt, Map.of());
        memoryService.store(runId, taskId, agentType(), "RESPONSE", response.content(),
                Map.of("model", response.model(), "tokens", response.promptTokens() + response.completionTokens()));

        Map<String, Object> structured = parseResponse(response.content());

        return new AgentOutput(
                response.content(),
                structured,
                response.promptTokens() + response.completionTokens(),
                elapsed
        );
    }

    @Override
    public String agentType() {
        return "RECOVERY";
    }

    private String buildPrompt(AgentInput input) {
        StringBuilder sb = new StringBuilder("Analyze the following task failure:\n\n");

        if (input.context().containsKey("errorMessage")) {
            sb.append("Error: ").append(input.context().get("errorMessage")).append("\n");
        }
        if (input.context().containsKey("taskType")) {
            sb.append("Task Type: ").append(input.context().get("taskType")).append("\n");
        }
        if (input.context().containsKey("attempt")) {
            sb.append("Attempt: ").append(input.context().get("attempt")).append("\n");
        }
        if (input.context().containsKey("taskConfig")) {
            sb.append("Config: ").append(input.context().get("taskConfig")).append("\n");
        }

        if (input.prompt() != null && !input.prompt().isBlank()) {
            sb.append("\nAdditional context: ").append(input.prompt());
        }

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(String content) {
        try {
            return objectMapper.readValue(content, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse recovery response as JSON: {}", e.getMessage());
            return Map.of("rawResponse", content);
        }
    }
}
