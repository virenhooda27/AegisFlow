package com.aegisflow.task.executor;

import com.aegisflow.agent.entity.ApprovalRequest;
import com.aegisflow.agent.entity.ApprovalStatus;
import com.aegisflow.agent.repository.ApprovalRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class ApprovalTaskExecutor implements TaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(ApprovalTaskExecutor.class);

    private final ApprovalRequestRepository approvalRepository;

    public ApprovalTaskExecutor(ApprovalRequestRepository approvalRepository) {
        this.approvalRepository = approvalRepository;
    }

    @Override
    public TaskResult execute(TaskContext context) {
        log.info("ApprovalTaskExecutor: task '{}' requires human approval", context.nodeKey());

        Map<String, Object> config = context.config();
        String title = (String) config.getOrDefault("title", "Approval Required: " + context.nodeName());
        String description = (String) config.getOrDefault("description", "");

        UUID taskRunId = config.containsKey("taskRunId")
                ? UUID.fromString(config.get("taskRunId").toString()) : null;
        UUID workflowRunId = config.containsKey("workflowRunId")
                ? UUID.fromString(config.get("workflowRunId").toString()) : null;

        if (taskRunId == null || workflowRunId == null) {
            return TaskResult.failure("Approval task requires taskRunId and workflowRunId in config");
        }

        // Create approval request
        ApprovalRequest approval = ApprovalRequest.builder()
                .workflowRunId(workflowRunId)
                .taskRunId(taskRunId)
                .title(title)
                .description(description)
                .status(ApprovalStatus.PENDING)
                .build();
        approvalRepository.save(approval);

        // Return failure so the task stays in a non-terminal state
        // The task will be retried; on retry it checks if approval was granted
        var existing = approvalRepository.findByTaskRunId(taskRunId);
        if (existing.isPresent()) {
            ApprovalRequest req = existing.get();
            if (req.getStatus() == ApprovalStatus.APPROVED) {
                return TaskResult.success(Map.of(
                        "approvalId", req.getId().toString(),
                        "approvedBy", req.getResolvedBy() != null ? req.getResolvedBy() : "unknown"
                ));
            } else if (req.getStatus() == ApprovalStatus.REJECTED) {
                return TaskResult.failure("Approval rejected" +
                        (req.getResolutionNote() != null ? ": " + req.getResolutionNote() : ""));
            }
        }

        // PENDING — throw a specific exception to signal the orchestrator to pause this task
        return TaskResult.failure("APPROVAL_PENDING:" + approval.getId());
    }

    @Override
    public String getType() {
        return "APPROVAL";
    }
}
