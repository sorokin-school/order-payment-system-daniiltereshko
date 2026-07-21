package dev.sorokin.async;

import dev.sorokin.async.task.entity.TaskEntity;
import dev.sorokin.async.task.service.TaskCompletionService;
import dev.sorokin.async.task.type.TaskExecutionStatus;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AsyncTaskDispatcher {

    private final TaskCompletionService taskCompletionService;
    private final AsyncTaskProcessor processor;
    private final ErrorTypeClassifier errorTypeClassifier;
    private final ThreadPoolTaskExecutor taskThreadPool;

    public AsyncTaskDispatcher(
            TaskCompletionService taskCompletionService,
            AsyncTaskProcessor processor,
            ErrorTypeClassifier errorTypeClassifier,
            @Qualifier("taskThreadPool") ThreadPoolTaskExecutor taskThreadPool
    ) {
        this.taskCompletionService = taskCompletionService;
        this.processor = processor;
        this.errorTypeClassifier = errorTypeClassifier;
        this.taskThreadPool = taskThreadPool;
    }

    public void dispatch(TaskEntity task) {
        try {
            CompletableFuture.runAsync(() -> processor.process(task), taskThreadPool)
                    .exceptionally(ex -> handlePipelineException(task, unwrap(ex)));
        } catch (RejectedExecutionException ex) {
            handleDispatchRejection(task, ex);
        }
    }

    private void handleDispatchRejection(TaskEntity task, RejectedExecutionException ex) {
        log.warn("Task dispatch rejected, returning to retry queue: taskId={}, error={}",
                task.getId(), ex.getMessage());
        taskCompletionService.completeRetryable(task.getId());
    }

    private Void handlePipelineException(TaskEntity task, Throwable ex) {
        log.error("Task execution failed: taskId={}, error={}", task.getId(), ex.getMessage(), ex);
        TaskExecutionStatus status = errorTypeClassifier.classify(ex);
        UUID taskId = task.getId();
        UUID orderId = task.getOrder().getId();
        switch (status) {
            case SUCCEEDED -> throw new IllegalStateException("Unexpected SUCCEEDED from exception classifier");
            case FAILED_RETRYABLE -> taskCompletionService.completeRetryable(taskId);
            case FAILED_NON_RETRYABLE -> taskCompletionService.completeNonRetryable(taskId, orderId, null);
        }
        return null;
    }

    private static Throwable unwrap(Throwable ex) {
        if (ex instanceof CompletionException completionException && completionException.getCause() != null) {
            return completionException.getCause();
        }
        return ex;
    }
}
