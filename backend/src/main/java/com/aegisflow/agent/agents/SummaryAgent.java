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
public class SummaryAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(SummaryAgent.class);
    private static final String SYSTEM_PROMPT = """
            You are a Summary Agent for a workflow orchestration platform.
            Your role is to generate concise, actionable summaries of workflow executions.
            
            Given execution context, respond with JSON containing:
            - summary: a concise paragraph describing the execution
            - highlights: array of key observations
            - recommendations: array of actionable suggestions
            
            Respond ONLY with valid JSON, no markdown formatting.
            """;

    private final LLMProviderRegistry providerRegistry;
    private final AgentMemoryService memoryService;
    private final ObjectMapper objectMapper;

    public SummaryAgent(LLMProviderRegistry providerRegistry,
                         AgentMemoryService memoryService,
                         ObjectMapper objectMapper) {
        this.providerRegistry = providerRegistry;
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentOutput execute(AgentInput input) {
        long startMs = System.currentTimeMillis();
        log.info("SummaryAgent generating summary...");

        String prompt = buildPrompt(input);
        LLMProvider provider = providerRegistry.getDefault();
        LLMProvider.LLMResponse response = provider.complete(
                new LLMProvider.LLMRequest(prompt, SYSTEM_PROMPT, 0.5, 2048, Map.of()));

        long elapsed = System.currentTimeMillis() - startMs;

        UUID runId = input.context().containsKey("workflowRunId")
                ? UUID.fromString(input.context().get("workflowRunId").toString()) : null;

        memoryService.store(runId, null, agentType(), "PROMPT", prompt, Map.of());
        memoryService.store(runId, null, agentType(), "RESPONSE", response.content(),
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
        return "SUMMARY";
    }

    private String buildPrompt(AgentInput input) {
        StringBuilder sb = new StringBuilder("Summarize the following workflow execution:\n\n");

        if (input.context().containsKey("workflowName")) {
            sb.append("Workflow: ").append(input.context().get("workflowName")).append("\n");
        }
        if (input.context().containsKey("status")) {
            sb.append("Status: ").append(input.context().get("status")).append("\n");
        }
        if (input.context().containsKey("taskCount")) {
            sb.append("Tasks: ").append(input.context().get("taskCount")).append("\n");
        }
        if (input.context().containsKey("duration")) {
            sb.append("Duration: ").append(input.context().get("duration")).append("\n");
        }
        if (input.context().containsKey("errors")) {
            sb.append("Errors: ").append(input.context().get("errors")).append("\n");
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
            log.warn("Failed to parse summary response as JSON: {}", e.getMessage());
            return Map.of("rawResponse", content);
        }
    }
}
