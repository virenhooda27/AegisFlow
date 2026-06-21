package com.aegisflow.agent.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_request")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workflow_run_id", nullable = false)
    private UUID workflowRunId;

    @Column(name = "task_run_id", nullable = false)
    private UUID taskRunId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (requestedAt == null) requestedAt = Instant.now();
        if (status == null) status = ApprovalStatus.PENDING;
    }
}
