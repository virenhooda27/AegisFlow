package com.aegisflow.execution.service;

import com.aegisflow.common.exception.ResourceNotFoundException;
import com.aegisflow.execution.entity.RunStatus;
import com.aegisflow.execution.entity.TaskRun;
import com.aegisflow.execution.entity.WorkflowRun;
import com.aegisflow.execution.repository.TaskRunRepository;
import com.aegisflow.execution.repository.WorkflowRunRepository;
import com.aegisflow.workflow.entity.WorkflowDefinition;
import com.aegisflow.workflow.entity.WorkflowNode;
import com.aegisflow.workflow.repository.WorkflowDefinitionRepository;
import com.aegisflow.workflow.repository.WorkflowNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkflowRunService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowRunService.class);

    private final WorkflowRunRepository runRepository;
    private final TaskRunRepository taskRunRepository;
    private final WorkflowDefinitionRepository workflowRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final OrchestrationEngine orchestrationEngine;
    private final ExecutionEventService eventService;

    public WorkflowRunService(WorkflowRunRepository runRepository,
                               TaskRunRepository taskRunRepository,
                               WorkflowDefinitionRepository workflowRepository,
                               WorkflowNodeRepository nodeRepository,
                               OrchestrationEngine orchestrationEngine,
                               ExecutionEventService eventService) {
        this.runRepository = runRepository;
        this.taskRunRepository = taskRunRepository;
        this.workflowRepository = workflowRepository;
        this.nodeRepository = nodeRepository;
        this.orchestrationEngine = orchestrationEngine;
        this.eventService = eventService;
    }

    @Transactional
    public WorkflowRun startRun(UUID workflowId) {
        WorkflowDefinition workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDefinition", workflowId));

        WorkflowRun run = WorkflowRun.builder()
                .workflow(workflow)
                .status(RunStatus.CREATED)
                .build();
        run = runRepository.save(run);

        log.info("Created workflow run {} for workflow '{}' v{}", run.getId(), workflow.getName(), workflow.getVersion());
        eventService.recordRunEvent(run, "RUN_CREATED", null, RunStatus.CREATED, null);

        // Create task runs for each node
        List<WorkflowNode> nodes = nodeRepository.findByWorkflowId(workflowId);
        for (WorkflowNode node : nodes) {
            int maxAttempts = 3;
            Map<String, Object> retryPolicy = node.getRetryPolicy();
            if (retryPolicy != null && retryPolicy.containsKey("maxAttempts")) {
                maxAttempts = ((Number) retryPolicy.get("maxAttempts")).intValue();
            }

            TaskRun taskRun = TaskRun.builder()
                    .workflowRun(run)
                    .workflowNode(node)
                    .nodeKey(node.getNodeKey())
                    .status(RunStatus.CREATED)
                    .maxAttempts(maxAttempts)
                    .build();
            taskRunRepository.save(taskRun);
        }

        // Advance to find initially ready tasks
        orchestrationEngine.advanceRun(run.getId());

        return runRepository.findById(run.getId()).orElseThrow();
    }

    @Transactional(readOnly = true)
    public WorkflowRun getRun(UUID runId) {
        return runRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowRun", runId));
    }

    @Transactional(readOnly = true)
    public List<WorkflowRun> getAllRuns() {
        return runRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<TaskRun> getTaskRuns(UUID runId) {
        return taskRunRepository.findByWorkflowRunId(runId);
    }

    @Transactional
    public WorkflowRun pauseRun(UUID runId) {
        WorkflowRun run = getRun(runId);
        if (run.getStatus() != RunStatus.RUNNING) {
            throw new IllegalStateException("Can only pause a RUNNING workflow run");
        }
        RunStatus previous = run.getStatus();
        run.setStatus(RunStatus.PAUSED);
        runRepository.save(run);
        eventService.recordRunEvent(run, "RUN_PAUSED", previous, RunStatus.PAUSED, null);
        log.info("Paused workflow run {}", runId);
        return run;
    }

    @Transactional
    public WorkflowRun resumeRun(UUID runId) {
        WorkflowRun run = getRun(runId);
        if (run.getStatus() != RunStatus.PAUSED) {
            throw new IllegalStateException("Can only resume a PAUSED workflow run");
        }
        RunStatus previous = run.getStatus();
        run.setStatus(RunStatus.RUNNING);
        runRepository.save(run);
        eventService.recordRunEvent(run, "RUN_RESUMED", previous, RunStatus.RUNNING, null);
        log.info("Resumed workflow run {}", runId);
        orchestrationEngine.advanceRun(runId);
        return run;
    }

    @Transactional
    public WorkflowRun cancelRun(UUID runId) {
        WorkflowRun run = getRun(runId);
        if (run.getStatus().isTerminal()) {
            throw new IllegalStateException("Cannot cancel a terminal workflow run");
        }
        RunStatus previous = run.getStatus();
        run.setStatus(RunStatus.CANCELLED);
        run.setCompletedAt(Instant.now());
        runRepository.save(run);
        eventService.recordRunEvent(run, "RUN_CANCELLED", previous, RunStatus.CANCELLED, null);
        log.info("Cancelled workflow run {}", runId);
        return run;
    }

    @Transactional
    public WorkflowRun retryRun(UUID runId) {
        WorkflowRun run = getRun(runId);
        if (run.getStatus() != RunStatus.FAILED) {
            throw new IllegalStateException("Can only retry a FAILED workflow run");
        }

        // Reset failed tasks to CREATED
        List<TaskRun> failedTasks = taskRunRepository.findByWorkflowRunIdAndStatus(runId, RunStatus.FAILED);
        for (TaskRun task : failedTasks) {
            task.setStatus(RunStatus.CREATED);
            task.setAttempt(0);
            task.setErrorMessage(null);
            task.setCompletedAt(null);
            task.setStartedAt(null);
            task.setAssignedWorkerId(null);
            taskRunRepository.save(task);
        }

        RunStatus previous = run.getStatus();
        run.setStatus(RunStatus.RUNNING);
        run.setCompletedAt(null);
        run.setErrorMessage(null);
        runRepository.save(run);
        eventService.recordRunEvent(run, "RUN_RETRIED", previous, RunStatus.RUNNING,
                "Retrying " + failedTasks.size() + " failed tasks");
        log.info("Retrying workflow run {} with {} failed tasks", runId, failedTasks.size());

        orchestrationEngine.advanceRun(runId);
        return run;
    }
}
