package com.aegisflow.agent.repository;

import com.aegisflow.agent.entity.AgentMemory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentMemoryRepository extends JpaRepository<AgentMemory, UUID> {

    List<AgentMemory> findByWorkflowRunIdOrderByCreatedAtDesc(UUID workflowRunId);

    List<AgentMemory> findByTaskRunIdOrderByCreatedAtDesc(UUID taskRunId);

    List<AgentMemory> findByAgentTypeAndMemoryTypeOrderByCreatedAtDesc(String agentType, String memoryType);

    List<AgentMemory> findByWorkflowRunIdAndAgentType(UUID workflowRunId, String agentType);
}
