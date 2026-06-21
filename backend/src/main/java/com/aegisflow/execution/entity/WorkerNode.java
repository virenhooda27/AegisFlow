package com.aegisflow.execution.entity;

import com.aegisflow.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "worker_node")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerNode extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "last_heartbeat", nullable = false)
    @Builder.Default
    private Instant lastHeartbeat = Instant.now();

    @Column(name = "active_tasks", nullable = false)
    @Builder.Default
    private Integer activeTasks = 0;

    @Column(name = "max_tasks", nullable = false)
    @Builder.Default
    private Integer maxTasks = 5;
}
