package com.aegisflow.agent.service;

import com.aegisflow.agent.entity.AgentMemory;
import com.aegisflow.agent.repository.AgentMemoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AgentMemoryService {

    private final AgentMemoryRepository memoryRepository;

    public AgentMemoryService(AgentMemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    public AgentMemory store(UUID workflowRunId, UUID taskRunId, String agentType,
                              String memoryType, String content, Map<String, Object> metadata) {
        AgentMemory memory = AgentMemory.builder()
                .workflowRunId(workflowRunId)
                .taskRunId(taskRunId)
                .agentType(agentType)
                .memoryType(memoryType)
                .content(content)
                .metadata(metadata)
                .build();
        return memoryRepository.save(memory);
    }

    public List<AgentMemory> getByRun(UUID workflowRunId) {
        return memoryRepository.findByWorkflowRunIdOrderByCreatedAtDesc(workflowRunId);
    }

    public List<AgentMemory> getByTask(UUID taskRunId) {
        return memoryRepository.findByTaskRunIdOrderByCreatedAtDesc(taskRunId);
    }

    public List<AgentMemory> getByRunAndAgent(UUID workflowRunId, String agentType) {
        return memoryRepository.findByWorkflowRunIdAndAgentType(workflowRunId, agentType);
    }
}
