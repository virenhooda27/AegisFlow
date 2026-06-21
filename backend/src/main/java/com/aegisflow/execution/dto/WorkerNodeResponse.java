package com.aegisflow.execution.dto;

import java.time.Instant;
import java.util.UUID;

public record WorkerNodeResponse(
        UUID id,
        String name,
        String status,
        Instant lastHeartbeat,
        Integer activeTasks,
        Integer maxTasks
) {}
