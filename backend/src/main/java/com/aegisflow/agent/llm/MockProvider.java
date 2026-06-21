package com.aegisflow.agent.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MockProvider implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(MockProvider.class);

    @Override
    public LLMResponse complete(LLMRequest request) {
        log.info("MockProvider processing prompt ({} chars)", request.prompt().length());

        String response = generateMockResponse(request.prompt());

        return new LLMResponse(
                response,
                "mock-v1",
                estimateTokens(request.prompt()),
                estimateTokens(response),
                Map.of("provider", "mock", "latencyMs", 50)
        );
    }

    @Override
    public String providerName() {
        return "mock";
    }

    private String generateMockResponse(String prompt) {
        String lower = prompt.toLowerCase();

        if (lower.contains("root cause") || lower.contains("failure") || lower.contains("error")) {
            return """
                    {
                      "rootCause": "NETWORK",
                      "confidence": 0.85,
                      "analysis": "The task failed due to a network connectivity issue. The target endpoint was unreachable.",
                      "recoveryPlan": [
                        "Verify network connectivity to the target endpoint",
                        "Check firewall rules and security groups",
                        "Retry the task with exponential backoff"
                      ]
                    }""";
        }

        if (lower.contains("summary") || lower.contains("summarize")) {
            return """
                    {
                      "summary": "The workflow executed successfully with 3 tasks completing in sequence. Total execution time was 12 seconds.",
                      "highlights": [
                        "All tasks completed on first attempt",
                        "No errors or warnings detected",
                        "Performance within expected parameters"
                      ],
                      "recommendations": [
                        "Consider adding retry policies for HTTP tasks",
                        "Monitor execution times for performance regression"
                      ]
                    }""";
        }

        if (lower.contains("plan") || lower.contains("design") || lower.contains("architect")) {
            return """
                    {
                      "plan": [
                        "Step 1: Analyze requirements and constraints",
                        "Step 2: Design the component architecture",
                        "Step 3: Implement core functionality",
                        "Step 4: Write unit and integration tests",
                        "Step 5: Review and refine implementation"
                      ],
                      "estimatedEffort": "2 hours",
                      "risks": ["Dependency conflicts", "Performance bottlenecks"]
                    }""";
        }

        return """
                {
                  "result": "Mock LLM response for the given prompt.",
                  "status": "success",
                  "note": "This is a mock provider. Configure aegisflow.llm.provider to use a real LLM."
                }""";
    }

    private int estimateTokens(String text) {
        return Math.max(1, text.length() / 4);
    }
}
