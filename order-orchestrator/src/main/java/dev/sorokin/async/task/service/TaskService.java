package dev.sorokin.async.task.service;

import dev.sorokin.async.task.entity.TaskEntity;
import dev.sorokin.async.task.repository.TaskJpaRepository;
import dev.sorokin.domain.OrderEntity;
import java.time.OffsetDateTime;
import java.util.List;
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
    public List<TaskEntity> fetchTasksForProcessing(final int batchSize, final OffsetDateTime inProgressTimeoutThreshold) {
        return repository.findTasksForProcessing(inProgressTimeoutThreshold, batchSize);
    }
}
