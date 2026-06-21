package com.aegisflow.agent.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MockProviderTest {

    private final MockProvider provider = new MockProvider();

    @Test
    void shouldReturnRecoveryResponseForFailurePrompt() {
        LLMProvider.LLMResponse response = provider.complete(
                new LLMProvider.LLMRequest("Analyze this failure: connection timeout error"));

        assertNotNull(response.content());
        assertTrue(response.content().contains("rootCause"));
        assertTrue(response.content().contains("NETWORK"));
        assertEquals("mock-v1", response.model());
        assertTrue(response.promptTokens() > 0);
        assertTrue(response.completionTokens() > 0);
    }

    @Test
    void shouldReturnSummaryResponseForSummaryPrompt() {
        LLMProvider.LLMResponse response = provider.complete(
                new LLMProvider.LLMRequest("Summarize this workflow execution"));

        assertNotNull(response.content());
        assertTrue(response.content().contains("summary"));
        assertTrue(response.content().contains("recommendations"));
    }

    @Test
    void shouldReturnPlanResponseForDesignPrompt() {
        LLMProvider.LLMResponse response = provider.complete(
                new LLMProvider.LLMRequest("Design a new authentication system"));

        assertNotNull(response.content());
        assertTrue(response.content().contains("plan"));
    }

    @Test
    void shouldReturnGenericResponseForUnknownPrompt() {
        LLMProvider.LLMResponse response = provider.complete(
                new LLMProvider.LLMRequest("Hello world"));

        assertNotNull(response.content());
        assertTrue(response.content().contains("Mock LLM response"));
    }

    @Test
    void shouldReturnCorrectProviderName() {
        assertEquals("mock", provider.providerName());
    }
}
