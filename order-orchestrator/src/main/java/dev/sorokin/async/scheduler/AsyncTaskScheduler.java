package dev.sorokin.async.scheduler;

import dev.sorokin.async.config.TaskAsyncProperties;
import dev.sorokin.async.task.entity.TaskEntity;
import dev.sorokin.async.task.service.TaskService;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncTaskScheduler {

    private final TaskService tasksService;
    private final TaskAsyncProperties taskAsyncProperties;

    @Scheduled(fixedDelayString = "${app.async.task.scheduler.fixed-delay-ms}")
    public void process() {
        final var batchSize = taskAsyncProperties.getBatchSize();
        final var inProgressTimeoutThreshold = OffsetDateTime.now()
                .minusMinutes(taskAsyncProperties.getInProgressTimeoutMinutes());

        List<TaskEntity> batch = tasksService.fetchTasksForProcessing(batchSize, inProgressTimeoutThreshold);
        if (batch.isEmpty()) {
            return;
        }

        for (TaskEntity task : batch) {
            log.info("Get task with id='{}'", task.getId());
        }
    }
}
