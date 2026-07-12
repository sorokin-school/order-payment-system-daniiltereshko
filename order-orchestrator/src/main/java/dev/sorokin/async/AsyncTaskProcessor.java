package dev.sorokin.async;

import dev.sorokin.async.task.entity.TaskEntity;
import dev.sorokin.async.task.type.TaskExecutionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AsyncTaskProcessor {

    public TaskExecutionStatus process(TaskEntity task) {
      log.info("Start process task with id={}", task.getId());
      return TaskExecutionStatus.SUCCEEDED;
    }
}
