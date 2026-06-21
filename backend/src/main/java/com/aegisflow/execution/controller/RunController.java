package com.aegisflow.execution.controller;

import com.aegisflow.execution.dto.StartRunRequest;
import com.aegisflow.execution.dto.TaskRunResponse;
import com.aegisflow.execution.dto.WorkerNodeResponse;
import com.aegisflow.execution.dto.WorkflowRunResponse;
import com.aegisflow.execution.entity.TaskRun;
import com.aegisflow.execution.entity.WorkflowRun;
import com.aegisflow.execution.mapper.RunMapper;
import com.aegisflow.execution.service.WorkerService;
import com.aegisflow.execution.service.WorkflowRunService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/runs")
public class RunController {

    private final WorkflowRunService runService;
    private final WorkerService workerService;
    private final RunMapper runMapper;

    public RunController(WorkflowRunService runService,
                          WorkerService workerService,
                          RunMapper runMapper) {
        this.runService = runService;
        this.workerService = workerService;
        this.runMapper = runMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowRunResponse startRun(@Valid @RequestBody StartRunRequest request) {
        WorkflowRun run = runService.startRun(request.workflowId());
        return buildResponse(run);
    }

    @GetMapping
    public List<WorkflowRunResponse> getAllRuns() {
        return runService.getAllRuns().stream()
                .map(this::buildResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public WorkflowRunResponse getRun(@PathVariable UUID id) {
        WorkflowRun run = runService.getRun(id);
        return buildResponse(run);
    }

    @GetMapping("/{id}/tasks")
    public List<TaskRunResponse> getTaskRuns(@PathVariable UUID id) {
        return runMapper.toTaskRunResponses(runService.getTaskRuns(id));
    }

    @PostMapping("/{id}/pause")
    public WorkflowRunResponse pauseRun(@PathVariable UUID id) {
        return buildResponse(runService.pauseRun(id));
    }

    @PostMapping("/{id}/resume")
    public WorkflowRunResponse resumeRun(@PathVariable UUID id) {
        return buildResponse(runService.resumeRun(id));
    }

    @PostMapping("/{id}/cancel")
    public WorkflowRunResponse cancelRun(@PathVariable UUID id) {
        return buildResponse(runService.cancelRun(id));
    }

    @PostMapping("/{id}/retry")
    public WorkflowRunResponse retryRun(@PathVariable UUID id) {
        return buildResponse(runService.retryRun(id));
    }

    @GetMapping("/workers")
    public List<WorkerNodeResponse> getWorkers() {
        return runMapper.toWorkerResponses(workerService.getAllWorkers());
    }

    private WorkflowRunResponse buildResponse(WorkflowRun run) {
        List<TaskRun> tasks = runService.getTaskRuns(run.getId());
        WorkflowRunResponse response = runMapper.toRunResponse(run);
        return new WorkflowRunResponse(
                response.id(),
                response.workflowId(),
                response.workflowName(),
                response.workflowVersion(),
                response.status(),
                response.startedAt(),
                response.completedAt(),
                response.errorMessage(),
                runMapper.toTaskRunResponses(tasks),
                response.createdAt()
        );
    }
}
