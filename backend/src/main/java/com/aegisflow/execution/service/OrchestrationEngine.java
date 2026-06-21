package com.aegisflow.execution.service;

import com.aegisflow.execution.entity.RunStatus;
import com.aegisflow.execution.entity.TaskRun;
import com.aegisflow.execution.entity.WorkflowRun;
import com.aegisflow.execution.repository.TaskRunRepository;
import com.aegisflow.execution.repository.WorkflowRunRepository;
import com.aegisflow.task.executor.TaskExecutor;
import com.aegisflow.task.executor.TaskExecutorRegistry;
import com.aegisflow.workflow.entity.WorkflowEdge;
import com.aegisflow.workflow.entity.WorkflowNode;
import com.aegisflow.workflow.repository.WorkflowEdgeRepository;
import com.aegisflow.workflow.repository.WorkflowNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrchestrationEngine {

    private static final Logger log = LoggerFactory.getLogger(OrchestrationEngine.class);

    private final WorkflowRunRepository runRepository;
    private final TaskRunRepository taskRunRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowEdgeRepository edgeRepository;
    private final TaskExecutorRegistry executorRegistry;
    private final ExecutionEventService eventService;
    private final RunNotificationService notificationService;

    public OrchestrationEngine(WorkflowRunRepository runRepository,
                                TaskRunRepository taskRunRepository,
                                WorkflowNodeRepository nodeRepository,
                                WorkflowEdgeRepository edgeRepository,
                                TaskExecutorRegistry executorRegistry,
                                ExecutionEventService eventService,
                                RunNotificationService notificationService) {
        this.runRepository = runRepository;
        this.taskRunRepository = taskRunRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.executorRegistry = executorRegistry;
        this.eventService = eventService;
        this.notificationService = notificationService;
    }

    @Transactional
    public void advanceRun(UUID runId) {
        WorkflowRun run = runRepository.findById(runId).orElse(null);
        if (run == null || run.getStatus().isTerminal() || run.getStatus() == RunStatus.PAUSED) {
            return;
        }

        List<TaskRun> tasks = taskRunRepository.findByWorkflowRunId(runId);
        UUID workflowId = run.getWorkflow().getId();
        List<WorkflowEdge> edges = edgeRepository.findByWorkflowId(workflowId);

        Map<String, TaskRun> taskByKey = tasks.stream()
                .collect(Collectors.toMap(TaskRun::getNodeKey, t -> t));

        Map<String, Set<String>> upstreamMap = buildUpstreamMap(edges, workflowId);

        // Check for overall failure (PAUSED tasks are NOT failures — they're awaiting approval)
        boolean anyFailed = tasks.stream()
                .anyMatch(t -> t.getStatus() == RunStatus.FAILED);
        if (anyFailed) {
            completeRun(run, RunStatus.FAILED, "One or more tasks failed");
            return;
        }

        // Check for overall success (all must be SUCCEEDED, not just non-FAILED)
        boolean allSucceeded = tasks.stream()
                .allMatch(t -> t.getStatus() == RunStatus.SUCCEEDED);
        if (allSucceeded) {
            completeRun(run, RunStatus.SUCCEEDED, null);
            return;
        }

        // If any tasks are paused (awaiting approval), don't mark new tasks as ready past them
        boolean anyPaused = tasks.stream()
                .anyMatch(t -> t.getStatus() == RunStatus.PAUSED);
        if (anyPaused) {
            log.debug("Run {} has paused tasks awaiting approval, not advancing further", runId);
        }

        // Find and mark READY tasks
        for (TaskRun task : tasks) {
            if (task.getStatus() != RunStatus.CREATED) {
                continue;
            }

            Set<String> upstreams = upstreamMap.getOrDefault(task.getNodeKey(), Set.of());
            boolean allUpstreamDone = upstreams.stream()
                    .allMatch(key -> {
                        TaskRun upstream = taskByKey.get(key);
                        return upstream != null && upstream.getStatus() == RunStatus.SUCCEEDED;
                    });

            if (allUpstreamDone) {
                task.setStatus(RunStatus.READY);
                taskRunRepository.save(task);
                eventService.recordTaskEvent(run, task, "TASK_READY",
                        RunStatus.CREATED, RunStatus.READY, "All upstream tasks completed");
                log.info("Task '{}' in run {} is READY", task.getNodeKey(), runId);
                notificationService.notifyTaskStatusChange(runId, task);
            }
        }

        // Ensure run is RUNNING
        if (run.getStatus() == RunStatus.CREATED) {
            run.setStatus(RunStatus.RUNNING);
            run.setStartedAt(Instant.now());
            runRepository.save(run);
            eventService.recordRunEvent(run, "RUN_STARTED", RunStatus.CREATED, RunStatus.RUNNING, null);
            notificationService.notifyRunStatusChange(run, tasks);
        }
    }

    @Transactional
    public void executeTask(UUID taskRunId, UUID workerId) {
        TaskRun task = taskRunRepository.findById(taskRunId).orElse(null);
        if (task == null || task.getStatus() != RunStatus.READY) {
            return;
        }

        WorkflowRun run = task.getWorkflowRun();
        WorkflowNode node = task.getWorkflowNode();

        // Mark as RUNNING
        RunStatus previousStatus = task.getStatus();
        task.setStatus(RunStatus.RUNNING);
        task.setStartedAt(Instant.now());
        task.setAttempt(task.getAttempt() + 1);
        task.setAssignedWorkerId(workerId);
        taskRunRepository.save(task);
        eventService.recordTaskEvent(run, task, "TASK_STARTED",
                previousStatus, RunStatus.RUNNING, "Attempt " + task.getAttempt());
        notificationService.notifyTaskStatusChange(run.getId(), task);

        log.info("Executing task '{}' (attempt {}) in run {}", task.getNodeKey(), task.getAttempt(), run.getId());

        // Execute via the appropriate executor
        try {
            TaskExecutor executor = executorRegistry.getExecutor(node.getType());

            // Inject runtime IDs into config for executors that need them (e.g. APPROVAL)
            Map<String, Object> enrichedConfig = new HashMap<>(node.getConfig() != null ? node.getConfig() : Map.of());
            enrichedConfig.put("taskRunId", task.getId().toString());
            enrichedConfig.put("workflowRunId", run.getId().toString());

            TaskExecutor.TaskContext context2 = new TaskExecutor.TaskContext(
                    node.getNodeKey(),
                    node.getName(),
                    enrichedConfig,
                    node.getTimeoutSeconds(),
                    task.getAttempt()
            );

            TaskExecutor.TaskResult result = executor.execute(context2);

            if (result.success()) {
                task.setStatus(RunStatus.SUCCEEDED);
                task.setCompletedAt(Instant.now());
                task.setOutput(result.output());
                taskRunRepository.save(task);
                eventService.recordTaskEvent(run, task, "TASK_SUCCEEDED",
                        RunStatus.RUNNING, RunStatus.SUCCEEDED, null);
                notificationService.notifyTaskStatusChange(run.getId(), task);
                log.info("Task '{}' succeeded in run {}", task.getNodeKey(), run.getId());
            } else if (result.errorMessage() != null && result.errorMessage().startsWith("APPROVAL_PENDING:")) {
                // Approval task needs human intervention — pause the task
                task.setStatus(RunStatus.PAUSED);
                task.setErrorMessage(null);
                taskRunRepository.save(task);
                eventService.recordTaskEvent(run, task, "TASK_PAUSED",
                        RunStatus.RUNNING, RunStatus.PAUSED, "Waiting for human approval");
                notificationService.notifyTaskStatusChange(run.getId(), task);
                log.info("Task '{}' paused for approval in run {}", task.getNodeKey(), run.getId());
            } else {
                handleTaskFailure(run, task, result.errorMessage());
            }
        } catch (Exception e) {
            log.error("Task '{}' threw exception: {}", task.getNodeKey(), e.getMessage(), e);
            handleTaskFailure(run, task, e.getMessage());
        }

        // Advance the run after task completion
        advanceRun(run.getId());
    }

    private void handleTaskFailure(WorkflowRun run, TaskRun task, String errorMessage) {
        if (task.getAttempt() < task.getMaxAttempts()) {
            // Retry
            task.setStatus(RunStatus.READY);
            task.setErrorMessage(errorMessage);
            taskRunRepository.save(task);
            eventService.recordTaskEvent(run, task, "TASK_RETRY",
                    RunStatus.RUNNING, RunStatus.READY,
                    "Retrying (" + task.getAttempt() + "/" + task.getMaxAttempts() + "): " + errorMessage);
            log.info("Task '{}' will retry ({}/{})", task.getNodeKey(), task.getAttempt(), task.getMaxAttempts());
            notificationService.notifyTaskStatusChange(run.getId(), task);
        } else {
            // Final failure
            task.setStatus(RunStatus.FAILED);
            task.setCompletedAt(Instant.now());
            task.setErrorMessage(errorMessage);
            taskRunRepository.save(task);
            eventService.recordTaskEvent(run, task, "TASK_FAILED",
                    RunStatus.RUNNING, RunStatus.FAILED,
                    "Max attempts reached: " + errorMessage);
            log.error("Task '{}' failed after {} attempts in run {}", task.getNodeKey(), task.getAttempt(), run.getId());
            notificationService.notifyTaskStatusChange(run.getId(), task);
        }
    }

    private void completeRun(WorkflowRun run, RunStatus status, String errorMessage) {
        RunStatus previous = run.getStatus();
        run.setStatus(status);
        run.setCompletedAt(Instant.now());
        run.setErrorMessage(errorMessage);
        runRepository.save(run);
        eventService.recordRunEvent(run,
                status == RunStatus.SUCCEEDED ? "RUN_SUCCEEDED" : "RUN_FAILED",
                previous, status, errorMessage);
        log.info("Workflow run {} completed with status {}", run.getId(), status);
        List<TaskRun> tasks = taskRunRepository.findByWorkflowRunId(run.getId());
        notificationService.notifyRunStatusChange(run, tasks);
    }

    private Map<String, Set<String>> buildUpstreamMap(List<WorkflowEdge> edges, UUID workflowId) {
        List<WorkflowNode> nodes = nodeRepository.findByWorkflowId(workflowId);
        Map<UUID, String> nodeIdToKey = nodes.stream()
                .collect(Collectors.toMap(n -> n.getId(), WorkflowNode::getNodeKey));

        Map<String, Set<String>> upstreamMap = new HashMap<>();
        for (WorkflowEdge edge : edges) {
            String sourceKey = nodeIdToKey.get(edge.getSourceNode().getId());
            String targetKey = nodeIdToKey.get(edge.getTargetNode().getId());
            if (sourceKey != null && targetKey != null) {
                upstreamMap.computeIfAbsent(targetKey, k -> new HashSet<>()).add(sourceKey);
            }
        }
        return upstreamMap;
    }
}
