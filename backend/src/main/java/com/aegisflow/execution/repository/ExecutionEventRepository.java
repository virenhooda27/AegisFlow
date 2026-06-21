package com.aegisflow.execution.repository;

import com.aegisflow.execution.entity.ExecutionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExecutionEventRepository extends JpaRepository<ExecutionEvent, UUID> {

    List<ExecutionEvent> findByWorkflowRunIdOrderByCreatedAtAsc(UUID workflowRunId);

    List<ExecutionEvent> findByTaskRunIdOrderByCreatedAtAsc(UUID taskRunId);
}
