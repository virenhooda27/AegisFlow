package com.aegisflow.workflow.entity;

import com.aegisflow.common.entity.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.Map;

@Entity
@Table(name = "workflow_node")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowNode extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private WorkflowDefinition workflow;

    @Column(name = "node_key", nullable = false)
    private String nodeKey;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    private String type;

    @Type(JsonType.class)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, Object> config;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds;

    @Type(JsonType.class)
    @Column(name = "retry_policy", columnDefinition = "jsonb")
    private Map<String, Object> retryPolicy;

    @Column(name = "position_x", nullable = false)
    @Builder.Default
    private Double positionX = 0.0;

    @Column(name = "position_y", nullable = false)
    @Builder.Default
    private Double positionY = 0.0;
}
