package com.aegisflow.workflow.controller;

import com.aegisflow.workflow.dto.ValidationResultDto;
import com.aegisflow.workflow.dto.WorkflowCreateRequest;
import com.aegisflow.workflow.dto.WorkflowResponse;
import com.aegisflow.workflow.dto.WorkflowUpdateRequest;
import com.aegisflow.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflows")
@Tag(name = "Workflows", description = "Workflow definition management")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    @Operation(summary = "Create a new workflow definition")
    public ResponseEntity<WorkflowResponse> createWorkflow(@Valid @RequestBody WorkflowCreateRequest request) {
        WorkflowResponse response = workflowService.createWorkflow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all workflows (latest versions)")
    public ResponseEntity<List<WorkflowResponse>> getAllWorkflows() {
        return ResponseEntity.ok(workflowService.getAllWorkflows());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a workflow by ID")
    public ResponseEntity<WorkflowResponse> getWorkflow(@PathVariable UUID id) {
        return ResponseEntity.ok(workflowService.getWorkflow(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a workflow (creates a new version)")
    public ResponseEntity<WorkflowResponse> updateWorkflow(@PathVariable UUID id,
                                                            @Valid @RequestBody WorkflowUpdateRequest request) {
        return ResponseEntity.ok(workflowService.updateWorkflow(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a workflow version")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable UUID id) {
        workflowService.deleteWorkflow(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/validate")
    @Operation(summary = "Validate workflow DAG structure")
    public ResponseEntity<ValidationResultDto> validateWorkflow(@PathVariable UUID id) {
        return ResponseEntity.ok(workflowService.validateWorkflow(id));
    }
}
