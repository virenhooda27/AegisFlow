package com.aegisflow.execution.service;

import com.aegisflow.execution.entity.WorkerNode;
import com.aegisflow.execution.repository.WorkerNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class WorkerService {

    private static final Logger log = LoggerFactory.getLogger(WorkerService.class);

    private final WorkerNodeRepository workerRepository;

    public WorkerService(WorkerNodeRepository workerRepository) {
        this.workerRepository = workerRepository;
    }

    @Transactional
    public WorkerNode registerWorker(String name, int maxTasks) {
        return workerRepository.findByName(name)
                .map(existing -> {
                    existing.setStatus("ACTIVE");
                    existing.setLastHeartbeat(Instant.now());
                    existing.setMaxTasks(maxTasks);
                    return workerRepository.save(existing);
                })
                .orElseGet(() -> {
                    WorkerNode worker = WorkerNode.builder()
                            .name(name)
                            .status("ACTIVE")
                            .maxTasks(maxTasks)
                            .build();
                    worker = workerRepository.save(worker);
                    log.info("Registered worker '{}' (id={})", name, worker.getId());
                    return worker;
                });
    }

    @Transactional
    public void heartbeat(UUID workerId) {
        workerRepository.findById(workerId).ifPresent(worker -> {
            worker.setLastHeartbeat(Instant.now());
            workerRepository.save(worker);
        });
    }

    @Transactional
    public void incrementActiveTasks(UUID workerId) {
        workerRepository.findById(workerId).ifPresent(worker -> {
            worker.setActiveTasks(worker.getActiveTasks() + 1);
            workerRepository.save(worker);
        });
    }

    @Transactional
    public void decrementActiveTasks(UUID workerId) {
        workerRepository.findById(workerId).ifPresent(worker -> {
            worker.setActiveTasks(Math.max(0, worker.getActiveTasks() - 1));
            workerRepository.save(worker);
        });
    }

    @Transactional(readOnly = true)
    public List<WorkerNode> getAvailableWorkers() {
        return workerRepository.findByStatusAndActiveTasksLessThan("ACTIVE", Integer.MAX_VALUE);
    }

    @Transactional(readOnly = true)
    public List<WorkerNode> getAllWorkers() {
        return workerRepository.findAll();
    }

    @Transactional
    public void markStaleWorkers() {
        Instant cutoff = Instant.now().minus(60, ChronoUnit.SECONDS);
        List<WorkerNode> stale = workerRepository.findByLastHeartbeatBefore(cutoff);
        for (WorkerNode worker : stale) {
            if ("ACTIVE".equals(worker.getStatus())) {
                worker.setStatus("STALE");
                workerRepository.save(worker);
                log.warn("Worker '{}' marked as STALE (no heartbeat since {})", worker.getName(), worker.getLastHeartbeat());
            }
        }
    }
}
