package com.aegisflow.agent.controller;

import com.aegisflow.agent.core.Agent;
import com.aegisflow.agent.core.AgentRegistry;
import com.aegisflow.agent.dto.AgentInvokeRequest;
import com.aegisflow.agent.dto.AgentResponse;
import com.aegisflow.agent.entity.AgentMemory;
import com.aegisflow.agent.service.AgentMemoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentRegistry agentRegistry;
    private final AgentMemoryService memoryService;

    public AgentController(AgentRegistry agentRegistry, AgentMemoryService memoryService) {
        this.agentRegistry = agentRegistry;
        this.memoryService = memoryService;
    }

    @PostMapping("/invoke")
    public ResponseEntity<AgentResponse> invoke(@RequestBody AgentInvokeRequest request) {
        Agent agent = agentRegistry.getAgent(request.agentType());

        Map<String, Object> context = request.context() != null ? request.context() : Map.of();
        if (request.workflowRunId() != null) {
            context = new java.util.HashMap<>(context);
            context.put("workflowRunId", request.workflowRunId().toString());
        }
        if (request.taskRunId() != null) {
            context = new java.util.HashMap<>(context);
            context.put("taskRunId", request.taskRunId().toString());
        }

        Agent.AgentInput input = new Agent.AgentInput(request.prompt(), context, Map.of());
        Agent.AgentOutput output = agent.execute(input);

        return ResponseEntity.ok(new AgentResponse(
                request.agentType(),
                output.result(),
                output.structuredOutput(),
                output.tokensUsed(),
                output.executionTimeMs()
        ));
    }

    @GetMapping("/types")
    public ResponseEntity<List<String>> getAgentTypes() {
        return ResponseEntity.ok(
                agentRegistry.getAllAgents().stream()
                        .map(Agent::agentType)
                        .toList()
        );
    }

    @GetMapping("/memory/run/{runId}")
    public ResponseEntity<List<AgentMemory>> getMemoryByRun(@PathVariable UUID runId) {
        return ResponseEntity.ok(memoryService.getByRun(runId));
    }

    @GetMapping("/memory/task/{taskId}")
    public ResponseEntity<List<AgentMemory>> getMemoryByTask(@PathVariable UUID taskId) {
        return ResponseEntity.ok(memoryService.getByTask(taskId));
    }
}
