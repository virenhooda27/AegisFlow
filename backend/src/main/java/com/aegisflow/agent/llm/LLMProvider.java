package com.aegisflow.agent.llm;

import java.util.List;
import java.util.Map;

public interface LLMProvider {

    record LLMRequest(
            String prompt,
            String systemPrompt,
            double temperature,
            int maxTokens,
            Map<String, Object> parameters
    ) {
        public LLMRequest(String prompt) {
            this(prompt, null, 0.7, 2048, Map.of());
        }

        public LLMRequest(String prompt, String systemPrompt) {
            this(prompt, systemPrompt, 0.7, 2048, Map.of());
        }
    }

    record LLMResponse(
            String content,
            String model,
            int promptTokens,
            int completionTokens,
            Map<String, Object> metadata
    ) {}

    LLMResponse complete(LLMRequest request);

    String providerName();
}
