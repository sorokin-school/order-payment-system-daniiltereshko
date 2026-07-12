package dev.sorokin.async.task.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskExecutionStatus {
    SUCCEEDED("SUCCEEDED"),
    FAILED_RETRYABLE("FAILED_RETRYABLE"),
    FAILED_NON_RETRYABLE("FAILED_NON_RETRYABLE");

    private final String name;
}
