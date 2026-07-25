package dev.sorokin.async.task.service;

import dev.sorokin.async.config.properties.TaskAsyncProperties;
import dev.sorokin.async.config.properties.TaskAsyncSchedulerProperties;
import dev.sorokin.async.task.entity.TaskEntity;
import dev.sorokin.async.task.repository.TaskJpaRepository;
import dev.sorokin.async.task.type.TaskStep;
import dev.sorokin.domain.OrderEntity;
import dev.sorokin.domain.OrderService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class TaskService {

    private final TaskJpaRepository repository;
    private final OrderService orderService;
    private final TaskAsyncSchedulerProperties schedulerProperties;
    private final TaskAsyncProperties taskAsyncProperties;

    public TaskService(
            TaskJpaRepository repository,
            @Lazy OrderService orderService,
            TaskAsyncSchedulerProperties schedulerProperties,
            TaskAsyncProperties taskAsyncProperties
    ) {
        this.repository = repository;
        this.orderService = orderService;
        this.schedulerProperties = schedulerProperties;
        this.taskAsyncProperties = taskAsyncProperties;
    }

    @Transactional
    public void createTaskForOrder(OrderEntity order) {
        var task = TaskEntity.createForOrder(order);
        var savedTask = repository.save(task);

        log.info("Task created successfully with id: {} for order: {}",
                savedTask.getId(), savedTask.getOrder().getId());
    }

    @Transactional
    public List<TaskEntity> claimTasks(final int batchSize) {
        List<UUID> lockedIds = repository.lockTaskIdsForProcessing(OffsetDateTime.now(), batchSize);
        if (lockedIds.isEmpty()) {
            return List.of();
        }

        OffsetDateTime lockedUntil = OffsetDateTime.now()
                .plusMinutes(schedulerProperties.getInProgressTimeoutMinutes());

        List<TaskEntity> batch = repository.findAllByIdInWithOrder(lockedIds);

        List<TaskEntity> claimed = new ArrayList<>(batch.size());
        for (TaskEntity task : batch) {
            if (hasExceededMaxAttempts(task)) {
                task.markFailedNonRetryable();
                log.warn("Task exceeded max attempts, skipping claim: taskId={}, attempts={}, maxAttempts={}",
                        task.getId(), task.getAttempts(), taskAsyncProperties.getMaxAttempts());
                continue;
            }
            task.markInProgress(lockedUntil);
            claimed.add(task);
        }

        return claimed;
    }

    @Transactional
    public void extendLock(UUID taskId) {
        TaskEntity task = requireTask(taskId);
        OffsetDateTime lockedUntil = OffsetDateTime.now()
                .plusMinutes(schedulerProperties.getInProgressTimeoutMinutes());
        task.extendLock(lockedUntil);
        repository.save(task);
    }

    @Transactional
    public void advanceStep(UUID taskId, TaskStep step) {
        TaskEntity task = requireTask(taskId);
        task.advanceStep(step);
        repository.save(task);
    }

    @Transactional
    public void markCaptureInProgress(UUID taskId, UUID orderId) {
        orderService.markAwaitingCapture(orderId);

        TaskEntity task = requireTask(taskId);
        task.advanceStep(TaskStep.CAPTURE);
        repository.save(task);
    }

    @Transactional(readOnly = true)
    public TaskEntity findTask(UUID taskId) {
        return requireTask(taskId);
    }

    private boolean hasExceededMaxAttempts(TaskEntity task) {
        return task.getAttempts() >= taskAsyncProperties.getMaxAttempts();
    }

    private TaskEntity requireTask(UUID taskId) {
        return repository.findById(taskId)
                .orElseThrow(() -> new IllegalStateException("Task not found: " + taskId));
    }
}
