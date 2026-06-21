package com.aegisflow.workflow.repository;

import com.aegisflow.workflow.entity.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID> {

    Optional<WorkflowDefinition> findTopByNameOrderByVersionDesc(String name);

    List<WorkflowDefinition> findByName(String name);

    @Query("SELECT COALESCE(MAX(w.version), 0) FROM WorkflowDefinition w WHERE w.name = :name")
    int findMaxVersionByName(@Param("name") String name);

    @Query("SELECT w FROM WorkflowDefinition w WHERE w.id IN " +
           "(SELECT w2.id FROM WorkflowDefinition w2 WHERE w2.version = " +
           "(SELECT MAX(w3.version) FROM WorkflowDefinition w3 WHERE w3.name = w2.name))")
    List<WorkflowDefinition> findAllLatestVersions();
}
