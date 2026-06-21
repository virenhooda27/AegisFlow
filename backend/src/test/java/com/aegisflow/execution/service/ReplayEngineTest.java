package com.aegisflow.execution.service;

import com.aegisflow.execution.entity.ExecutionEvent;
import com.aegisflow.execution.entity.RunStatus;
import com.aegisflow.execution.entity.TaskRun;
import com.aegisflow.execution.entity.WorkflowRun;
import com.aegisflow.execution.repository.ExecutionEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplayEngineTest {

    @Mock private ExecutionEventRepository eventRepository;

    private ReplayEngine replayEngine;

    @BeforeEach
    void setUp() {
        replayEngine = new ReplayEngine(eventRepository);
    }

    @Test
    void shouldReplayRunFromEvents() {
        UUID runId = UUID.randomUUID();
        WorkflowRun run = new WorkflowRun();
        run.setId(runId);

        TaskRun taskA = new TaskRun();
        taskA.setNodeKey("taskA");

        TaskRun taskB = new TaskRun();
        taskB.setNodeKey("taskB");

        List<ExecutionEvent> events = List.of(
                buildEvent(run, null, "RUN_STARTED", "CREATED", "RUNNING"),
                buildEvent(run, taskA, "TASK_READY", "CREATED", "READY"),
                buildEvent(run, taskA, "TASK_STARTED", "READY", "RUNNING"),
                buildEvent(run, taskA, "TASK_SUCCEEDED", "RUNNING", "SUCCEEDED"),
                buildEvent(run, taskB, "TASK_READY", "CREATED", "READY"),
                buildEvent(run, taskB, "TASK_STARTED", "READY", "RUNNING"),
                buildEvent(run, taskB, "TASK_SUCCEEDED", "RUNNING", "SUCCEEDED"),
                buildEvent(run, null, "RUN_SUCCEEDED", "RUNNING", "SUCCEEDED")
        );

        when(eventRepository.findByWorkflowRunIdOrderByCreatedAtAsc(runId)).thenReturn(events);

        ReplayEngine.ReplayState state = replayEngine.replay(runId);

        assertEquals(runId, state.runId());
        assertEquals(RunStatus.SUCCEEDED, state.runStatus());
        assertEquals(2, state.tasks().size());
        assertEquals(RunStatus.SUCCEEDED, state.tasks().get("taskA").status());
        assertEquals(RunStatus.SUCCEEDED, state.tasks().get("taskB").status());
        assertEquals(8, state.timeline().size());
    }

    @Test
    void shouldHandleEmptyEvents() {
        UUID runId = UUID.randomUUID();
        when(eventRepository.findByWorkflowRunIdOrderByCreatedAtAsc(runId)).thenReturn(List.of());

        ReplayEngine.ReplayState state = replayEngine.replay(runId);

        assertEquals(RunStatus.CREATED, state.runStatus());
        assertTrue(state.tasks().isEmpty());
        assertTrue(state.timeline().isEmpty());
    }

    @Test
    void shouldTrackRetries() {
        UUID runId = UUID.randomUUID();
        WorkflowRun run = new WorkflowRun();
        run.setId(runId);

        TaskRun task = new TaskRun();
        task.setNodeKey("flaky");

        List<ExecutionEvent> events = List.of(
                buildEvent(run, null, "RUN_STARTED", "CREATED", "RUNNING"),
                buildEvent(run, task, "TASK_STARTED", "READY", "RUNNING"),
                buildEventWithMsg(run, task, "TASK_RETRY", "RUNNING", "READY", "Retrying (1/3)"),
                buildEvent(run, task, "TASK_STARTED", "READY", "RUNNING"),
                buildEvent(run, task, "TASK_SUCCEEDED", "RUNNING", "SUCCEEDED"),
                buildEvent(run, null, "RUN_SUCCEEDED", "RUNNING", "SUCCEEDED")
        );

        when(eventRepository.findByWorkflowRunIdOrderByCreatedAtAsc(runId)).thenReturn(events);

        ReplayEngine.ReplayState state = replayEngine.replay(runId);

        assertEquals(RunStatus.SUCCEEDED, state.runStatus());
        assertEquals(2, state.tasks().get("flaky").attempt());
    }

    private ExecutionEvent buildEvent(WorkflowRun run, TaskRun task,
                                       String eventType, String from, String to) {
        return ExecutionEvent.builder()
                .workflowRun(run)
                .taskRun(task)
                .eventType(eventType)
                .statusFrom(from)
                .statusTo(to)
                .createdAt(Instant.now())
                .build();
    }

    private ExecutionEvent buildEventWithMsg(WorkflowRun run, TaskRun task,
                                              String eventType, String from, String to, String msg) {
        return ExecutionEvent.builder()
                .workflowRun(run)
                .taskRun(task)
                .eventType(eventType)
                .statusFrom(from)
                .statusTo(to)
                .message(msg)
                .createdAt(Instant.now())
                .build();
    }
}
