package com.aegisflow.task.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
public class HttpTaskExecutor implements TaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(HttpTaskExecutor.class);

    private final HttpClient httpClient;

    public HttpTaskExecutor() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String getType() {
        return "HTTP";
    }

    @Override
    public TaskResult execute(TaskContext context) {
        Map<String, Object> config = context.config();
        String url = (String) config.getOrDefault("url", "");
        String method = (String) config.getOrDefault("method", "GET");
        String body = (String) config.getOrDefault("body", "");
        int timeout = context.timeoutSeconds() != null ? context.timeoutSeconds() : 30;

        if (url.isBlank()) {
            return TaskResult.failure("HTTP task requires 'url' in config");
        }

        log.info("Executing HTTP {} {} (attempt {})", method, url, context.attempt());

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeout));

            @SuppressWarnings("unchecked")
            Map<String, String> headers = (Map<String, String>) config.getOrDefault("headers", Map.of());
            headers.forEach(requestBuilder::header);

            HttpRequest request = switch (method.toUpperCase()) {
                case "POST" -> requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body)).build();
                case "PUT" -> requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body)).build();
                case "DELETE" -> requestBuilder.DELETE().build();
                default -> requestBuilder.GET().build();
            };

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            Map<String, Object> output = Map.of(
                    "statusCode", response.statusCode(),
                    "body", response.body() != null ? response.body() : ""
            );

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return TaskResult.success(output);
            } else {
                return TaskResult.failure("HTTP request failed with status " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("HTTP task execution failed: {}", e.getMessage(), e);
            return TaskResult.failure("HTTP request failed: " + e.getMessage());
        }
    }
}
