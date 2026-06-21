package com.aegisflow.task.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TaskExecutorRegistry {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutorRegistry.class);

    private final Map<String, TaskExecutor> executors;

    public TaskExecutorRegistry(List<TaskExecutor> executorList) {
        this.executors = executorList.stream()
                .collect(Collectors.toMap(TaskExecutor::getType, Function.identity()));
        log.info("Registered {} task executors: {}", executors.size(), executors.keySet());
    }

    public TaskExecutor getExecutor(String type) {
        TaskExecutor executor = executors.get(type);
        if (executor == null) {
            throw new IllegalArgumentException("No task executor registered for type: " + type);
        }
        return executor;
    }

    public boolean hasExecutor(String type) {
        return executors.containsKey(type);
    }
}
