package com.aegisflow.agent.agents;

import com.aegisflow.agent.core.Agent;
import com.aegisflow.agent.llm.LLMProviderRegistry;
import com.aegisflow.agent.llm.MockProvider;
import com.aegisflow.agent.service.AgentMemoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SummaryAgentTest {

    @Mock private AgentMemoryService memoryService;

    private SummaryAgent summaryAgent;

    @BeforeEach
    void setUp() {
        MockProvider mockProvider = new MockProvider();
        LLMProviderRegistry registry = new LLMProviderRegistry(List.of(mockProvider));
        summaryAgent = new SummaryAgent(registry, memoryService, new ObjectMapper());
    }

    @Test
    void shouldGenerateSummaryWithStructuredOutput() {
        Agent.AgentInput input = new Agent.AgentInput(
                "Summarize this execution",
                Map.of(
                        "workflowRunId", UUID.randomUUID().toString(),
                        "workflowName", "Data Pipeline",
                        "status", "SUCCEEDED",
                        "taskCount", 5,
                        "duration", "12s"
                ),
                Map.of()
        );

        Agent.AgentOutput output = summaryAgent.execute(input);

        assertNotNull(output.result());
        assertNotNull(output.structuredOutput());
        assertTrue(output.structuredOutput().containsKey("summary"));
        assertTrue(output.tokensUsed() > 0);

        verify(memoryService).store(any(), isNull(), eq("SUMMARY"), eq("PROMPT"), anyString(), any());
        verify(memoryService).store(any(), isNull(), eq("SUMMARY"), eq("RESPONSE"), anyString(), any());
    }

    @Test
    void shouldReturnCorrectAgentType() {
        assertEquals("SUMMARY", summaryAgent.agentType());
    }
}
