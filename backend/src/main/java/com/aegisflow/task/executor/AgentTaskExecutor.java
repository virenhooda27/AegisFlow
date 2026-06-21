package com.aegisflow.task.executor;

import com.aegisflow.agent.core.Agent;
import com.aegisflow.agent.core.AgentRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AgentTaskExecutor implements TaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(AgentTaskExecutor.class);

    private final AgentRegistry agentRegistry;

    public AgentTaskExecutor(AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
    }

    @Override
    public TaskResult execute(TaskContext context) {
        log.info("AgentTaskExecutor executing task '{}'", context.nodeKey());

        Map<String, Object> config = context.config();
        String agentType = (String) config.getOrDefault("agentType", "SUMMARY");
        String prompt = (String) config.getOrDefault("prompt", "");

        try {
            Agent agent = agentRegistry.getAgent(agentType);
            Agent.AgentInput input = new Agent.AgentInput(
                    prompt,
                    config,
                    Map.of("nodeKey", context.nodeKey(), "nodeName", context.nodeName())
            );

            Agent.AgentOutput output = agent.execute(input);

            return TaskResult.success(Map.of(
                    "result", output.result(),
                    "structuredOutput", output.structuredOutput(),
                    "tokensUsed", output.tokensUsed(),
                    "executionTimeMs", output.executionTimeMs()
            ));
        } catch (Exception e) {
            log.error("Agent task failed: {}", e.getMessage(), e);
            return TaskResult.failure("Agent execution failed: " + e.getMessage());
        }
    }

    @Override
    public String getType() {
        return "AGENT";
    }
}
