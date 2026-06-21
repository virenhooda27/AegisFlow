package com.aegisflow.agent.repository;

import com.aegisflow.agent.entity.ApprovalRequest;
import com.aegisflow.agent.entity.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {

    List<ApprovalRequest> findByStatusOrderByRequestedAtDesc(ApprovalStatus status);

    List<ApprovalRequest> findByWorkflowRunIdOrderByRequestedAtDesc(UUID workflowRunId);

    Optional<ApprovalRequest> findByTaskRunId(UUID taskRunId);
}
