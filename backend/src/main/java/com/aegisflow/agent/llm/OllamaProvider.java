package com.aegisflow.agent.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "aegisflow.llm.provider", havingValue = "ollama")
public class OllamaProvider implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaProvider.class);

    private final String baseUrl;
    private final String model;
    private final RestTemplate restTemplate;

    public OllamaProvider(org.springframework.core.env.Environment env) {
        this.baseUrl = env.getProperty("aegisflow.llm.ollama.base-url", "http://localhost:11434");
        this.model = env.getProperty("aegisflow.llm.ollama.model", "llama3");
        this.restTemplate = new RestTemplate();
        log.info("Ollama provider initialized: {} model={}", baseUrl, model);
    }

    @Override
    @SuppressWarnings("unchecked")
    public LLMResponse complete(LLMRequest request) {
        String url = baseUrl + "/api/generate";

        String fullPrompt = request.systemPrompt() != null
                ? request.systemPrompt() + "\n\n" + request.prompt()
                : request.prompt();

        Map<String, Object> body = Map.of(
                "model", model,
                "prompt", fullPrompt,
                "stream", false,
                "options", Map.of(
                        "temperature", request.temperature(),
                        "num_predict", request.maxTokens()
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
            throw new RuntimeException("Empty response from Ollama");
        }

        String content = (String) responseBody.get("response");
        int promptTokens = responseBody.containsKey("prompt_eval_count")
                ? ((Number) responseBody.get("prompt_eval_count")).intValue()
                : fullPrompt.length() / 4;
        int completionTokens = responseBody.containsKey("eval_count")
                ? ((Number) responseBody.get("eval_count")).intValue()
                : content.length() / 4;

        return new LLMResponse(content, model, promptTokens, completionTokens,
                Map.of("provider", "ollama", "latencyMs", latencyMs));
    }

    @Override
    public String providerName() {
        return "ollama";
    }
}
