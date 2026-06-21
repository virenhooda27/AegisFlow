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

        // Check for existing approval first (handles retries without creating duplicates)
        if (taskRunId != null) {
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
                // Still PENDING — signal the orchestrator to pause
                return TaskResult.failure("APPROVAL_PENDING:" + req.getId());
            }
        }

        if (taskRunId == null || workflowRunId == null) {
            return TaskResult.failure("Approval task requires taskRunId and workflowRunId in config");
        }

        // Create new approval request
        ApprovalRequest approval = ApprovalRequest.builder()
                .workflowRunId(workflowRunId)
                .taskRunId(taskRunId)
                .title(title)
                .description(description)
                .status(ApprovalStatus.PENDING)
                .build();
        approvalRepository.save(approval);

        // Signal the orchestrator to pause this task
        return TaskResult.failure("APPROVAL_PENDING:" + approval.getId());
    }

    @Override
    public String getType() {
        return "APPROVAL";
    }
}
