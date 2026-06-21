package com.aegisflow.task.executor;

import java.util.Map;

public interface TaskExecutor {

    String getType();

    TaskResult execute(TaskContext context);

    record TaskContext(
            String nodeKey,
            String nodeName,
            Map<String, Object> config,
            Integer timeoutSeconds,
            int attempt
    ) {}

    record TaskResult(
            boolean success,
            Map<String, Object> output,
            String errorMessage
    ) {
        public static TaskResult success(Map<String, Object> output) {
            return new TaskResult(true, output, null);
        }

        public static TaskResult failure(String errorMessage) {
            return new TaskResult(false, Map.of(), errorMessage);
        }
    }
}
