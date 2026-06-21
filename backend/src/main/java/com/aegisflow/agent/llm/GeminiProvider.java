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
@ConditionalOnProperty(name = "aegisflow.llm.provider", havingValue = "gemini")
public class GeminiProvider implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);

    private final String apiKey;
    private final String model;
    private final RestTemplate restTemplate;

    public GeminiProvider(org.springframework.core.env.Environment env) {
        this.apiKey = env.getProperty("aegisflow.llm.gemini.api-key", "");
        this.model = env.getProperty("aegisflow.llm.gemini.model", "gemini-1.5-flash");
        this.restTemplate = new RestTemplate();
        log.info("Gemini provider initialized with model: {}", model);
    }

    @Override
    @SuppressWarnings("unchecked")
    public LLMResponse complete(LLMRequest request) {
        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                model, apiKey);

        String fullPrompt = request.systemPrompt() != null
                ? request.systemPrompt() + "\n\n" + request.prompt()
                : request.prompt();

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", fullPrompt))
                )),
                "generationConfig", Map.of(
                        "temperature", request.temperature(),
                        "maxOutputTokens", request.maxTokens()
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        long startMs = System.currentTimeMillis();
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        long latencyMs = System.currentTimeMillis() - startMs;

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new RuntimeException("Empty response from Gemini API");
        }

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
        Map<String, Object> contentObj = (Map<String, Object>) candidates.getFirst().get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) contentObj.get("parts");
        String content = (String) parts.getFirst().get("text");

        int promptTokens = fullPrompt.length() / 4;
        int completionTokens = content.length() / 4;

        Map<String, Object> usageMetadata = (Map<String, Object>) responseBody.get("usageMetadata");
        if (usageMetadata != null) {
            promptTokens = ((Number) usageMetadata.getOrDefault("promptTokenCount", promptTokens)).intValue();
            completionTokens = ((Number) usageMetadata.getOrDefault("candidatesTokenCount", completionTokens)).intValue();
        }

        return new LLMResponse(content, model, promptTokens, completionTokens,
                Map.of("provider", "gemini", "latencyMs", latencyMs));
    }

    @Override
    public String providerName() {
        return "gemini";
    }
}
