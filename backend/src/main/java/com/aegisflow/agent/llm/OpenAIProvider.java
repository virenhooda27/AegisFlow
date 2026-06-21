package com.aegisflow.agent.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "aegisflow.llm.provider", havingValue = "openai")
public class OpenAIProvider implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAIProvider.class);
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    private final String apiKey;
    private final String model;
    private final RestTemplate restTemplate;

    public OpenAIProvider(
            org.springframework.core.env.Environment env) {
        this.apiKey = env.getProperty("aegisflow.llm.openai.api-key", "");
        this.model = env.getProperty("aegisflow.llm.openai.model", "gpt-4o-mini");
        this.restTemplate = new RestTemplate();
        log.info("OpenAI provider initialized with model: {}", model);
    }

    @Override
    @SuppressWarnings("unchecked")
    public LLMResponse complete(LLMRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        var messages = new java.util.ArrayList<Map<String, String>>();
        if (request.systemPrompt() != null) {
            messages.add(Map.of("role", "system", "content", request.systemPrompt()));
        }
        messages.add(Map.of("role", "user", "content", request.prompt()));

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "temperature", request.temperature(),
                "max_tokens", request.maxTokens()
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        long startMs = System.currentTimeMillis();
        ResponseEntity<Map> response = restTemplate.exchange(API_URL, HttpMethod.POST, entity, Map.class);
        long latencyMs = System.currentTimeMillis() - startMs;

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new RuntimeException("Empty response from OpenAI API");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.getFirst().get("message");
        String content = (String) message.get("content");

        Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
        int promptTokens = ((Number) usage.get("prompt_tokens")).intValue();
        int completionTokens = ((Number) usage.get("completion_tokens")).intValue();

        return new LLMResponse(content, model, promptTokens, completionTokens,
                Map.of("provider", "openai", "latencyMs", latencyMs));
    }

    @Override
    public String providerName() {
        return "openai";
    }
}
