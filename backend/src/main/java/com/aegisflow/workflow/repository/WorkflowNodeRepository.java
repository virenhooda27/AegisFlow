package com.aegisflow.workflow.repository;

import com.aegisflow.workflow.entity.WorkflowNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowNodeRepository extends JpaRepository<WorkflowNode, UUID> {

    List<WorkflowNode> findByWorkflowId(UUID workflowId);
}
