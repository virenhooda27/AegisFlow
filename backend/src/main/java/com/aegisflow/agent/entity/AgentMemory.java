package com.aegisflow.agent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "agent_memory")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AgentMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workflow_run_id")
    private UUID workflowRunId;

    @Column(name = "task_run_id")
    private UUID taskRunId;

    @Column(name = "agent_type", nullable = false, length = 50)
    private String agentType;

    @Column(name = "memory_type", nullable = false, length = 50)
    private String memoryType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
