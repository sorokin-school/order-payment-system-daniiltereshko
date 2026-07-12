package dev.sorokin.async;

import dev.sorokin.async.config.properties.TaskAsyncProperties;
import dev.sorokin.async.task.ErrorTypeClassifier;
import dev.sorokin.async.task.entity.TaskEntity;
import dev.sorokin.async.task.repository.TaskJpaRepository;
import dev.sorokin.async.task.type.TaskExecutionStatus;
import dev.sorokin.async.task.type.TaskStatus;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncTaskDispatcher {

    private final TaskJpaRepository repository;
    private final AsyncTaskProcessor processor;
    private final TaskAsyncProperties properties;
    private final ErrorTypeClassifier errorTypeClassifier;
    private final ThreadPoolTaskExecutor taskThreadPool;

    public void dispatch(TaskEntity task) {
        CompletableFuture.supplyAsync(() -> processor.process(task), taskThreadPool)
                .thenAccept(status -> handleTaskExecutionStatus(task, status))
                .exceptionally(ex -> handlePipelineException(task, ex));
    }

    private void handleTaskExecutionStatus(TaskEntity task, TaskExecutionStatus status) {
        log.info("Start handling task execution status: id={}, status={}, attempts={}",
                task.getId(), status, task.getAttempts());
        switch (status) {
            case SUCCEEDED -> handleSucceededStatus(task, status);
            case FAILED_RETRYABLE -> handleFailedRetryableStatus(task, status);
            case FAILED_NON_RETRYABLE -> handleFailedNonRetryableStatus(task, status);
        }
    }

    private void handleSucceededStatus(TaskEntity task, TaskExecutionStatus status) {
        task.setStatus(TaskStatus.SUCCEEDED);
        task.setNextAttemptAt(null);

        repository.save(task);

        log.info("Task completed successfully: taskId={}, status={}, attempts={}",
                task.getId(), status, task.getAttempts());
    }

    private void handleFailedRetryableStatus(TaskEntity task, TaskExecutionStatus status) {
        task.setStatus(TaskStatus.FAILED_RETRYABLE);
        task.setNextAttemptAt(OffsetDateTime.now().plusSeconds(properties.getRetryDelaySeconds()));

        repository.save(task);

        log.warn("Task failed (retryable): taskId={}, status={}, attempts={}, nextAttemptAt={}",
                task.getId(), status, task.getAttempts(), task.getNextAttemptAt());
    }

    private void handleFailedNonRetryableStatus(TaskEntity task, TaskExecutionStatus status) {
        task.setStatus(TaskStatus.FAILED_NON_RETRYABLE);
        task.setNextAttemptAt(null);

        repository.save(task);

        log.error("Task failed (non-retryable): taskId={}, status={}, attempts={}",
                task.getId(), status, task.getAttempts());
    }

    private Void handlePipelineException(TaskEntity task, Throwable ex) {
        log.error("Task execution failed: taskId={}, error={}", task.getId(), ex.getMessage(), ex);

        TaskExecutionStatus taskExecutionStatus = errorTypeClassifier.classify(ex);
        handleTaskExecutionStatus(task, taskExecutionStatus);

        return null;
    }
}
