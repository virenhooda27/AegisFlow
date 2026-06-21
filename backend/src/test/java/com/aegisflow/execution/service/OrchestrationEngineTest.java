package com.aegisflow.execution.service;

import com.aegisflow.execution.entity.RunStatus;
import com.aegisflow.execution.entity.TaskRun;
import com.aegisflow.execution.entity.WorkflowRun;
import com.aegisflow.execution.repository.TaskRunRepository;
import com.aegisflow.execution.repository.WorkflowRunRepository;
import com.aegisflow.task.executor.TaskExecutor;
import com.aegisflow.task.executor.TaskExecutorRegistry;
import com.aegisflow.workflow.entity.WorkflowDefinition;
import com.aegisflow.workflow.entity.WorkflowEdge;
import com.aegisflow.workflow.entity.WorkflowNode;
import com.aegisflow.workflow.repository.WorkflowEdgeRepository;
import com.aegisflow.workflow.repository.WorkflowNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrchestrationEngineTest {

    @Mock private WorkflowRunRepository runRepository;
    @Mock private TaskRunRepository taskRunRepository;
    @Mock private WorkflowNodeRepository nodeRepository;
    @Mock private WorkflowEdgeRepository edgeRepository;
    @Mock private TaskExecutorRegistry executorRegistry;
    @Mock private ExecutionEventService eventService;
    @Mock private RunNotificationService notificationService;

    private OrchestrationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new OrchestrationEngine(runRepository, taskRunRepository,
                nodeRepository, edgeRepository, executorRegistry, eventService, notificationService);
    }

    @Test
    void shouldMarkRootTasksAsReady() {
        UUID runId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();

        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(workflowId);

        WorkflowRun run = WorkflowRun.builder().workflow(workflow).status(RunStatus.CREATED).build();
        run.setId(runId);

        WorkflowNode nodeA = new WorkflowNode();
        nodeA.setId(UUID.randomUUID());
        nodeA.setNodeKey("a");

        TaskRun taskA = TaskRun.builder()
                .workflowRun(run)
                .workflowNode(nodeA)
                .nodeKey("a")
                .status(RunStatus.CREATED)
                .build();
        taskA.setId(UUID.randomUUID());

        when(runRepository.findById(runId)).thenReturn(Optional.of(run));
        when(taskRunRepository.findByWorkflowRunId(runId)).thenReturn(List.of(taskA));
        when(edgeRepository.findByWorkflowId(workflowId)).thenReturn(List.of());
        when(nodeRepository.findByWorkflowId(workflowId)).thenReturn(List.of(nodeA));
        when(taskRunRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(runRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        engine.advanceRun(runId);

        assertThat(taskA.getStatus()).isEqualTo(RunStatus.READY);
    }

    @Test
    void shouldCompleteRunWhenAllTasksSucceeded() {
        UUID runId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();

        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(workflowId);

        WorkflowRun run = WorkflowRun.builder().workflow(workflow).status(RunStatus.RUNNING).build();
        run.setId(runId);

        TaskRun taskA = TaskRun.builder()
                .workflowRun(run)
                .nodeKey("a")
                .status(RunStatus.SUCCEEDED)
                .build();
        taskA.setId(UUID.randomUUID());

        when(runRepository.findById(runId)).thenReturn(Optional.of(run));
        when(taskRunRepository.findByWorkflowRunId(runId)).thenReturn(List.of(taskA));
        when(edgeRepository.findByWorkflowId(workflowId)).thenReturn(List.of());
        when(runRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        engine.advanceRun(runId);

        assertThat(run.getStatus()).isEqualTo(RunStatus.SUCCEEDED);
    }

    @Test
    void shouldFailRunWhenTaskFails() {
        UUID runId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();

        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(workflowId);

        WorkflowRun run = WorkflowRun.builder().workflow(workflow).status(RunStatus.RUNNING).build();
        run.setId(runId);

        TaskRun taskA = TaskRun.builder()
                .workflowRun(run)
                .nodeKey("a")
                .status(RunStatus.FAILED)
                .build();
        taskA.setId(UUID.randomUUID());

        when(runRepository.findById(runId)).thenReturn(Optional.of(run));
        when(taskRunRepository.findByWorkflowRunId(runId)).thenReturn(List.of(taskA));
        when(edgeRepository.findByWorkflowId(workflowId)).thenReturn(List.of());
        when(runRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        engine.advanceRun(runId);

        assertThat(run.getStatus()).isEqualTo(RunStatus.FAILED);
    }

    @Test
    void shouldExecuteTaskSuccessfully() {
        UUID taskId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();

        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(workflowId);

        WorkflowRun run = WorkflowRun.builder().workflow(workflow).status(RunStatus.RUNNING).build();
        run.setId(runId);

        WorkflowNode node = new WorkflowNode();
        node.setNodeKey("a");
        node.setName("Task A");
        node.setType("HTTP");
        node.setConfig(Map.of("url", "http://example.com"));
        node.setTimeoutSeconds(30);

        TaskRun task = TaskRun.builder()
                .workflowRun(run)
                .workflowNode(node)
                .nodeKey("a")
                .status(RunStatus.READY)
                .maxAttempts(3)
                .build();
        task.setId(taskId);

        TaskExecutor mockExecutor = mock(TaskExecutor.class);
        when(mockExecutor.execute(any())).thenReturn(TaskExecutor.TaskResult.success(Map.of("result", "ok")));

        when(taskRunRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(executorRegistry.getExecutor("HTTP")).thenReturn(mockExecutor);
        when(taskRunRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(runRepository.findById(runId)).thenReturn(Optional.of(run));
        when(taskRunRepository.findByWorkflowRunId(runId)).thenReturn(List.of(task));
        when(edgeRepository.findByWorkflowId(workflowId)).thenReturn(List.of());
        when(runRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        engine.executeTask(taskId, workerId);

        assertThat(task.getStatus()).isEqualTo(RunStatus.SUCCEEDED);
        assertThat(task.getAttempt()).isEqualTo(1);
    }

    @Test
    void shouldRetryTaskOnFailure() {
        UUID taskId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();

        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(workflowId);

        WorkflowRun run = WorkflowRun.builder().workflow(workflow).status(RunStatus.RUNNING).build();
        run.setId(runId);

        WorkflowNode node = new WorkflowNode();
        node.setNodeKey("a");
        node.setName("Task A");
        node.setType("HTTP");
        node.setConfig(Map.of());

        TaskRun task = TaskRun.builder()
                .workflowRun(run)
                .workflowNode(node)
                .nodeKey("a")
                .status(RunStatus.READY)
                .attempt(0)
                .maxAttempts(3)
                .build();
        task.setId(taskId);

        TaskExecutor mockExecutor = mock(TaskExecutor.class);
        when(mockExecutor.execute(any())).thenReturn(TaskExecutor.TaskResult.failure("connection refused"));

        when(taskRunRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(executorRegistry.getExecutor("HTTP")).thenReturn(mockExecutor);
        when(taskRunRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(runRepository.findById(runId)).thenReturn(Optional.of(run));
        when(taskRunRepository.findByWorkflowRunId(runId)).thenReturn(List.of(task));
        when(edgeRepository.findByWorkflowId(workflowId)).thenReturn(List.of());
        when(nodeRepository.findByWorkflowId(workflowId)).thenReturn(List.of(node));

        engine.executeTask(taskId, workerId);

        // Should be READY for retry, not FAILED
        assertThat(task.getStatus()).isEqualTo(RunStatus.READY);
        assertThat(task.getAttempt()).isEqualTo(1);
    }

    @Test
    void shouldSkipTerminalRun() {
        UUID runId = UUID.randomUUID();

        WorkflowRun run = WorkflowRun.builder().status(RunStatus.SUCCEEDED).build();
        run.setId(runId);

        when(runRepository.findById(runId)).thenReturn(Optional.of(run));

        engine.advanceRun(runId);

        verify(taskRunRepository, never()).findByWorkflowRunId(any());
    }
}
