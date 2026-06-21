package com.aegisflow.agent.service;

import com.aegisflow.agent.entity.ApprovalRequest;
import com.aegisflow.agent.entity.ApprovalStatus;
import com.aegisflow.agent.repository.ApprovalRequestRepository;
import com.aegisflow.common.exception.ResourceNotFoundException;
import com.aegisflow.execution.entity.RunStatus;
import com.aegisflow.execution.entity.TaskRun;
import com.aegisflow.execution.repository.TaskRunRepository;
import com.aegisflow.execution.service.OrchestrationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    private final ApprovalRequestRepository approvalRepository;
    private final TaskRunRepository taskRunRepository;
    private final OrchestrationEngine orchestrationEngine;

    public ApprovalService(ApprovalRequestRepository approvalRepository,
                            TaskRunRepository taskRunRepository,
                            OrchestrationEngine orchestrationEngine) {
        this.approvalRepository = approvalRepository;
        this.taskRunRepository = taskRunRepository;
        this.orchestrationEngine = orchestrationEngine;
    }

    public List<ApprovalRequest> getPendingApprovals() {
        return approvalRepository.findByStatusOrderByRequestedAtDesc(ApprovalStatus.PENDING);
    }

    public List<ApprovalRequest> getByRun(UUID runId) {
        return approvalRepository.findByWorkflowRunIdOrderByRequestedAtDesc(runId);
    }

    public ApprovalRequest getById(UUID id) {
        return approvalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRequest", id));
    }

    @Transactional
    public ApprovalRequest approve(UUID approvalId, String resolvedBy, String note) {
        ApprovalRequest approval = getById(approvalId);
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Approval is not pending");
        }

        approval.setStatus(ApprovalStatus.APPROVED);
        approval.setResolvedAt(Instant.now());
        approval.setResolvedBy(resolvedBy);
        approval.setResolutionNote(note);
        approvalRepository.save(approval);

        // Unblock the task — set it back to READY
        TaskRun task = taskRunRepository.findById(approval.getTaskRunId()).orElse(null);
        if (task != null && task.getStatus() == RunStatus.PAUSED) {
            task.setStatus(RunStatus.READY);
            task.setAttempt(0);
            task.setErrorMessage(null);
            taskRunRepository.save(task);
            orchestrationEngine.advanceRun(task.getWorkflowRun().getId());
        }

        log.info("Approval {} approved by {}", approvalId, resolvedBy);
        return approval;
    }

    @Transactional
    public ApprovalRequest reject(UUID approvalId, String resolvedBy, String note) {
        ApprovalRequest approval = getById(approvalId);
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Approval is not pending");
        }

        approval.setStatus(ApprovalStatus.REJECTED);
        approval.setResolvedAt(Instant.now());
        approval.setResolvedBy(resolvedBy);
        approval.setResolutionNote(note);
        approvalRepository.save(approval);

        // Fail the task
        TaskRun task = taskRunRepository.findById(approval.getTaskRunId()).orElse(null);
        if (task != null) {
            task.setStatus(RunStatus.FAILED);
            task.setErrorMessage("Approval rejected: " + (note != null ? note : ""));
            task.setCompletedAt(Instant.now());
            taskRunRepository.save(task);
            orchestrationEngine.advanceRun(task.getWorkflowRun().getId());
        }

        log.info("Approval {} rejected by {}", approvalId, resolvedBy);
        return approval;
    }
}
