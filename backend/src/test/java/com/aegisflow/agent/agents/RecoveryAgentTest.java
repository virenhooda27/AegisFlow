package com.aegisflow.agent.agents;

import com.aegisflow.agent.core.Agent;
import com.aegisflow.agent.llm.LLMProvider;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecoveryAgentTest {

    @Mock private AgentMemoryService memoryService;

    private RecoveryAgent recoveryAgent;

    @BeforeEach
    void setUp() {
        MockProvider mockProvider = new MockProvider();
        LLMProviderRegistry registry = new LLMProviderRegistry(List.of(mockProvider));
        recoveryAgent = new RecoveryAgent(registry, memoryService, new ObjectMapper());
    }

    @Test
    void shouldAnalyzeFailureAndReturnStructuredOutput() {
        Agent.AgentInput input = new Agent.AgentInput(
                "Connection timed out after 30s",
                Map.of(
                        "workflowRunId", UUID.randomUUID().toString(),
                        "taskRunId", UUID.randomUUID().toString(),
                        "errorMessage", "Connection timed out",
                        "taskType", "HTTP",
                        "attempt", 2
                ),
                Map.of()
        );

        Agent.AgentOutput output = recoveryAgent.execute(input);

        assertNotNull(output.result());
        assertNotNull(output.structuredOutput());
        assertTrue(output.structuredOutput().containsKey("rootCause"));
        assertTrue(output.tokensUsed() > 0);
        assertTrue(output.executionTimeMs() >= 0);

        // Verify memory was stored (prompt + response)
        verify(memoryService).store(any(), any(), eq("RECOVERY"), eq("PROMPT"), anyString(), any());
        verify(memoryService).store(any(), any(), eq("RECOVERY"), eq("RESPONSE"), anyString(), any());
    }

    @Test
    void shouldReturnCorrectAgentType() {
        assertEquals("RECOVERY", recoveryAgent.agentType());
    }
}
