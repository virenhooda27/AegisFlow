package com.aegisflow.execution.service;

import com.aegisflow.execution.entity.RunStatus;
import com.aegisflow.execution.entity.TaskRun;
import com.aegisflow.execution.entity.WorkerNode;
import com.aegisflow.execution.entity.WorkflowRun;
import com.aegisflow.execution.repository.TaskRunRepository;
import com.aegisflow.execution.repository.WorkflowRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TaskSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(TaskSchedulerService.class);

    private final TaskRunRepository taskRunRepository;
    private final WorkflowRunRepository runRepository;
    private final OrchestrationEngine orchestrationEngine;
    private final WorkerService workerService;
    private final ExecutorService taskExecutorPool;

    public TaskSchedulerService(TaskRunRepository taskRunRepository,
                          WorkflowRunRepository runRepository,
                          OrchestrationEngine orchestrationEngine,
                          WorkerService workerService) {
        this.taskRunRepository = taskRunRepository;
        this.runRepository = runRepository;
        this.orchestrationEngine = orchestrationEngine;
        this.workerService = workerService;
        this.taskExecutorPool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors());
    }

    @Scheduled(fixedDelay = 2000)
    public void pollAndExecuteReadyTasks() {
        List<TaskRun> readyTasks = taskRunRepository.findByStatusAndAssignedWorkerIdIsNull(RunStatus.READY);
        if (readyTasks.isEmpty()) {
            return;
        }

        List<WorkerNode> workers = workerService.getAvailableWorkers();
        if (workers.isEmpty()) {
            log.debug("No available workers for {} ready tasks", readyTasks.size());
            return;
        }

        WorkerNode worker = workers.getFirst();

        for (TaskRun task : readyTasks) {
            if (worker.getActiveTasks() >= worker.getMaxTasks()) {
                break;
            }

            log.info("Scheduling task '{}' on worker '{}'", task.getNodeKey(), worker.getName());
            workerService.incrementActiveTasks(worker.getId());

            taskExecutorPool.submit(() -> {
                try {
                    orchestrationEngine.executeTask(task.getId(), worker.getId());
                } finally {
                    workerService.decrementActiveTasks(worker.getId());
                    workerService.heartbeat(worker.getId());
                }
            });
        }
    }

    @Scheduled(fixedDelay = 10000)
    public void advanceActiveRuns() {
        List<WorkflowRun> activeRuns = runRepository.findByStatusIn(
                List.of(RunStatus.RUNNING, RunStatus.CREATED));
        for (WorkflowRun run : activeRuns) {
            orchestrationEngine.advanceRun(run.getId());
        }
    }

    @Scheduled(fixedDelay = 30000)
    public void checkWorkerHealth() {
        workerService.markStaleWorkers();
    }
}
