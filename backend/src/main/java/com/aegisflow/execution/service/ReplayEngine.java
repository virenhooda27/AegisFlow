package com.aegisflow.execution.service;

import com.aegisflow.execution.entity.ExecutionEvent;
import com.aegisflow.execution.entity.RunStatus;
import com.aegisflow.execution.repository.ExecutionEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ReplayEngine {

    private static final Logger log = LoggerFactory.getLogger(ReplayEngine.class);

    private final ExecutionEventRepository eventRepository;

    public ReplayEngine(ExecutionEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public record ReplayState(
            UUID runId,
            RunStatus runStatus,
            Map<String, TaskState> tasks,
            List<EventEntry> timeline
    ) {}

    public record TaskState(
            String nodeKey,
            RunStatus status,
            int attempt,
            String errorMessage
    ) {}

    public record EventEntry(
            String eventType,
            String nodeKey,
            RunStatus fromStatus,
            RunStatus toStatus,
            String message,
            String timestamp
    ) {}

    public ReplayState replay(UUID runId) {
        List<ExecutionEvent> events = eventRepository.findByWorkflowRunIdOrderByCreatedAtAsc(runId);
        if (events.isEmpty()) {
            log.warn("No events found for run {}", runId);
            return new ReplayState(runId, RunStatus.CREATED, Map.of(), List.of());
        }

        RunStatus runStatus = RunStatus.CREATED;
        Map<String, TaskState> tasks = new HashMap<>();
        List<EventEntry> timeline = new ArrayList<>();

        for (ExecutionEvent event : events) {
            String eventType = event.getEventType();
            String nodeKey = event.getTaskRun() != null ? event.getTaskRun().getNodeKey() : null;

            RunStatus fromStatus = parseStatus(event.getStatusFrom());
            RunStatus toStatus = parseStatus(event.getStatusTo());

            // Update run status
            if (eventType.startsWith("RUN_")) {
                runStatus = toStatus;
            }

            // Update task status
            if (nodeKey != null && toStatus != null) {
                int attempt = tasks.containsKey(nodeKey) ? tasks.get(nodeKey).attempt() : 0;
                if (eventType.contains("STARTED")) attempt++;
                String error = eventType.contains("FAILED") ? event.getMessage() : null;
                tasks.put(nodeKey, new TaskState(nodeKey, toStatus, attempt, error));
            }

            timeline.add(new EventEntry(
                    eventType, nodeKey, fromStatus, toStatus,
                    event.getMessage(),
                    event.getCreatedAt().toString()
            ));
        }

        log.info("Replayed run {} from {} events, final status: {}", runId, events.size(), runStatus);
        return new ReplayState(runId, runStatus, tasks, timeline);
    }

    private RunStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return RunStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
