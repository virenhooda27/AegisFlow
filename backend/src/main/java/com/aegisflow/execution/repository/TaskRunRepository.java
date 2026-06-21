package com.aegisflow.execution.repository;

import com.aegisflow.execution.entity.RunStatus;
import com.aegisflow.execution.entity.TaskRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRunRepository extends JpaRepository<TaskRun, UUID> {

    List<TaskRun> findByWorkflowRunId(UUID workflowRunId);

    List<TaskRun> findByWorkflowRunIdAndStatus(UUID workflowRunId, RunStatus status);

    List<TaskRun> findByStatusAndAssignedWorkerIdIsNull(RunStatus status);
}
