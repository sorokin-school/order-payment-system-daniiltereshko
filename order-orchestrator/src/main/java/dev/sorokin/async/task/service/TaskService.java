package dev.sorokin.async.task.service;

import dev.sorokin.async.task.entity.TaskEntity;
import dev.sorokin.async.task.repository.TaskJpaRepository;
import dev.sorokin.async.task.type.TaskStatus;
import dev.sorokin.domain.OrderEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskJpaRepository repository;

    @Transactional
    public void createTaskForOrder(OrderEntity order) {
        var task = TaskEntity.createForOrder(order);
        var savedTask = repository.save(task);

        log.info("Task created successfully with id: {} for order: {}",
                savedTask.getId(), savedTask.getOrder().getId());
    }

    @Transactional
    public List<TaskEntity> claimTasks(final int batchSize, final OffsetDateTime inProgressTimeoutThreshold) {
        List<UUID> lockedIds = repository.lockTaskIdsForProcessing(inProgressTimeoutThreshold, batchSize);
        if (lockedIds.isEmpty()) {
            return List.of();
        }
        List<TaskEntity> batch = repository.findAllByIdInWithOrder(lockedIds);

        for (TaskEntity task : batch) {
            task.setStatus(TaskStatus.IN_PROGRESS);
            task.setAttempts(task.getAttempts() + 1);
        }

        return batch;
    }
}
