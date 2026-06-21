package com.aegisflow.execution.entity;

public enum RunStatus {
    CREATED,
    READY,
    RUNNING,
    RETRYING,
    SUCCEEDED,
    FAILED,
    PAUSED,
    CANCELLED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELLED;
    }
}
