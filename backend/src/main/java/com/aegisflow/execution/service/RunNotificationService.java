package com.aegisflow.execution.service;

import com.aegisflow.execution.dto.TaskRunResponse;
import com.aegisflow.execution.dto.WorkflowRunResponse;
import com.aegisflow.execution.entity.TaskRun;
import com.aegisflow.execution.entity.WorkflowRun;
import com.aegisflow.execution.mapper.RunMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RunNotificationService {

    private static final Logger log = LoggerFactory.getLogger(RunNotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final RunMapper runMapper;

    public RunNotificationService(SimpMessagingTemplate messagingTemplate, RunMapper runMapper) {
        this.messagingTemplate = messagingTemplate;
        this.runMapper = runMapper;
    }

    public void notifyRunStatusChange(WorkflowRun run, List<TaskRun> tasks) {
        WorkflowRunResponse response = runMapper.toRunResponse(run);
        List<TaskRunResponse> taskResponses = runMapper.toTaskRunResponses(tasks);

        WorkflowRunResponse fullResponse = new WorkflowRunResponse(
                response.id(),
                response.workflowId(),
                response.workflowName(),
                response.workflowVersion(),
                response.status(),
                response.startedAt(),
                response.completedAt(),
                response.errorMessage(),
                taskResponses,
                response.createdAt()
        );

        // Broadcast to all subscribers of this run
        messagingTemplate.convertAndSend("/topic/runs/" + run.getId(), fullResponse);
        // Also broadcast to the runs list topic
        messagingTemplate.convertAndSend("/topic/runs", Map.of(
                "type", "RUN_UPDATE",
                "runId", run.getId().toString(),
                "status", run.getStatus().name()
        ));

        log.debug("Notified WebSocket: run {} status={}", run.getId(), run.getStatus());
    }

    public void notifyTaskStatusChange(UUID runId, TaskRun task) {
        TaskRunResponse response = runMapper.toTaskRunResponse(task);
        messagingTemplate.convertAndSend("/topic/runs/" + runId + "/tasks", response);

        log.debug("Notified WebSocket: task {} status={} in run {}", task.getNodeKey(), task.getStatus(), runId);
    }

    public void notifyWorkerUpdate() {
        messagingTemplate.convertAndSend("/topic/workers", Map.of("type", "WORKER_UPDATE"));
    }
}
