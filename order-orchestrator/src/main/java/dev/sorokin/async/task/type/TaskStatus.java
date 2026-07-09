package dev.sorokin.async.task.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskStatus {
    NEW("NEW"),
    IN_PROGRESS("IN_PROGRESS"),
    SUCCEEDED("SUCCEEDED"),
    FAILED_RETRYABLE("FAILED_RETRYABLE"),
    FAILED_NON_RETRYABLE("FAILED_NON_RETRYABLE");

    private final String name;
}
