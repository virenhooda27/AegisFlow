package com.aegisflow.execution.repository;

import com.aegisflow.execution.entity.WorkerNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkerNodeRepository extends JpaRepository<WorkerNode, UUID> {

    List<WorkerNode> findByStatusAndActiveTasksLessThan(String status, int maxTasks);

    Optional<WorkerNode> findByName(String name);

    List<WorkerNode> findByLastHeartbeatBefore(Instant cutoff);
}
