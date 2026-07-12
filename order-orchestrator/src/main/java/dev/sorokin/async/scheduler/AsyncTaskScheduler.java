package dev.sorokin.async.scheduler;

import dev.sorokin.async.AsyncTaskDispatcher;
import dev.sorokin.async.config.properties.TaskAsyncSchedulerProperties;
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
    private final AsyncTaskDispatcher taskDispatcher;
    private final TaskAsyncSchedulerProperties taskAsyncSchedulerProperties;

    @Scheduled(fixedDelayString = "${app.async.task.scheduler.fixed-delay-ms}")
    public void process() {
        List<TaskEntity> batch = fetchTaskForProcessing();
        if (batch.isEmpty()) {
            return;
        }

        for (TaskEntity task : batch) {
            taskDispatcher.dispatch(task);
        }
    }

    private List<TaskEntity> fetchTaskForProcessing() {
        final var batchSize = taskAsyncSchedulerProperties.getBatchSize();
        final var inProgressTimeoutThreshold = OffsetDateTime.now()
                .minusMinutes(taskAsyncSchedulerProperties.getInProgressTimeoutMinutes());

        return tasksService.claimTasks(batchSize, inProgressTimeoutThreshold);
    }
}
