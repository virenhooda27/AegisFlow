package com.aegisflow.task.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class ShellTaskExecutor implements TaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(ShellTaskExecutor.class);

    @Override
    public String getType() {
        return "SHELL";
    }

    @Override
    public TaskResult execute(TaskContext context) {
        Map<String, Object> config = context.config();
        String command = (String) config.getOrDefault("command", "");
        String workingDir = (String) config.getOrDefault("workingDir", ".");
        int timeout = context.timeoutSeconds() != null ? context.timeoutSeconds() : 60;

        if (command.isBlank()) {
            return TaskResult.failure("Shell task requires 'command' in config");
        }

        log.info("Executing shell command: {} (attempt {})", command, context.attempt());

        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            ProcessBuilder pb = isWindows
                    ? new ProcessBuilder("cmd", "/c", command)
                    : new ProcessBuilder("sh", "-c", command);

            pb.directory(new java.io.File(workingDir));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            boolean completed = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return TaskResult.failure("Shell command timed out after " + timeout + "s");
            }

            int exitCode = process.exitValue();
            Map<String, Object> result = Map.of(
                    "exitCode", exitCode,
                    "output", output
            );

            if (exitCode == 0) {
                return TaskResult.success(result);
            } else {
                return TaskResult.failure("Shell command exited with code " + exitCode + ": " + output);
            }
        } catch (Exception e) {
            log.error("Shell task execution failed: {}", e.getMessage(), e);
            return TaskResult.failure("Shell execution failed: " + e.getMessage());
        }
    }
}
