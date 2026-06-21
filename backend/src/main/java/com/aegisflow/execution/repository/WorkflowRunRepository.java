package com.aegisflow.execution.repository;

import com.aegisflow.execution.entity.RunStatus;
import com.aegisflow.execution.entity.WorkflowRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowRunRepository extends JpaRepository<WorkflowRun, UUID> {

    List<WorkflowRun> findByWorkflowIdOrderByCreatedAtDesc(UUID workflowId);

    List<WorkflowRun> findByStatusIn(List<RunStatus> statuses);

    List<WorkflowRun> findAllByOrderByCreatedAtDesc();
}
