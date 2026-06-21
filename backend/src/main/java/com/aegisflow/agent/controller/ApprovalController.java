package com.aegisflow.agent.controller;

import com.aegisflow.agent.dto.ApprovalActionRequest;
import com.aegisflow.agent.dto.ApprovalResponse;
import com.aegisflow.agent.entity.ApprovalRequest;
import com.aegisflow.agent.service.ApprovalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping
    public ResponseEntity<List<ApprovalResponse>> getPending() {
        return ResponseEntity.ok(approvalService.getPendingApprovals().stream()
                .map(this::toResponse).toList());
    }

    @GetMapping("/run/{runId}")
    public ResponseEntity<List<ApprovalResponse>> getByRun(@PathVariable UUID runId) {
        return ResponseEntity.ok(approvalService.getByRun(runId).stream()
                .map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApprovalResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(approvalService.getById(id)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApprovalResponse> approve(
            @PathVariable UUID id,
            @RequestBody(required = false) ApprovalActionRequest request) {
        String resolvedBy = request != null ? request.resolvedBy() : "anonymous";
        String note = request != null ? request.note() : null;
        return ResponseEntity.ok(toResponse(approvalService.approve(id, resolvedBy, note)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApprovalResponse> reject(
            @PathVariable UUID id,
            @RequestBody(required = false) ApprovalActionRequest request) {
        String resolvedBy = request != null ? request.resolvedBy() : "anonymous";
        String note = request != null ? request.note() : null;
        return ResponseEntity.ok(toResponse(approvalService.reject(id, resolvedBy, note)));
    }

    private ApprovalResponse toResponse(ApprovalRequest r) {
        return new ApprovalResponse(
                r.getId(), r.getWorkflowRunId(), r.getTaskRunId(),
                r.getTitle(), r.getDescription(), r.getStatus().name(),
                r.getRequestedAt(), r.getResolvedAt(), r.getResolvedBy(), r.getResolutionNote()
        );
    }
}
