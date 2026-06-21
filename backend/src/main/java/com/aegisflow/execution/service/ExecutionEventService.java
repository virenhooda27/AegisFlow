package com.aegisflow.execution.service;

import com.aegisflow.execution.entity.ExecutionEvent;
import com.aegisflow.execution.entity.RunStatus;
import com.aegisflow.execution.entity.TaskRun;
import com.aegisflow.execution.entity.WorkflowRun;
import com.aegisflow.execution.repository.ExecutionEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ExecutionEventService {

    private final ExecutionEventRepository eventRepository;

    public ExecutionEventService(ExecutionEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void recordRunEvent(WorkflowRun run, String eventType, RunStatus from, RunStatus to, String message) {
        ExecutionEvent event = ExecutionEvent.builder()
                .workflowRun(run)
                .eventType(eventType)
                .statusFrom(from != null ? from.name() : null)
                .statusTo(to != null ? to.name() : null)
                .message(message)
                .build();
        eventRepository.save(event);
    }

    public void recordTaskEvent(WorkflowRun run, TaskRun task, String eventType,
                                 RunStatus from, RunStatus to, String message) {
        ExecutionEvent event = ExecutionEvent.builder()
                .workflowRun(run)
                .taskRun(task)
                .eventType(eventType)
                .statusFrom(from != null ? from.name() : null)
                .statusTo(to != null ? to.name() : null)
                .message(message)
                .build();
        eventRepository.save(event);
    }

    public void recordTaskEvent(WorkflowRun run, TaskRun task, String eventType,
                                 RunStatus from, RunStatus to, String message, Map<String, Object> metadata) {
        ExecutionEvent event = ExecutionEvent.builder()
                .workflowRun(run)
                .taskRun(task)
                .eventType(eventType)
                .statusFrom(from != null ? from.name() : null)
                .statusTo(to != null ? to.name() : null)
                .message(message)
                .metadata(metadata)
                .build();
        eventRepository.save(event);
    }

    public List<ExecutionEvent> getEventsForRun(UUID runId) {
        return eventRepository.findByWorkflowRunIdOrderByCreatedAtAsc(runId);
    }

    public List<ExecutionEvent> getEventsForTask(UUID taskId) {
        return eventRepository.findByTaskRunIdOrderByCreatedAtAsc(taskId);
    }
}
