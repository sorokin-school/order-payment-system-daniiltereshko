package dev.sorokin.async;

import dev.sorokin.async.config.properties.TaskAsyncProperties;
import dev.sorokin.async.task.type.TaskExecutionStatus;
import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ErrorTypeClassifier {
    private static final int MAX_CAUSE_DEPTH = 10;

    private final Set<Class<?>> retryableExceptionClasses;
    private final Set<Class<?>> nonRetryableExceptionClasses;


    public ErrorTypeClassifier(TaskAsyncProperties properties) {
        this.retryableExceptionClasses = loadClasses(properties.getRetryableExceptions());
        this.nonRetryableExceptionClasses = loadClasses(properties.getNonRetryableExceptions());

        log.info("Loaded retryable exceptions: {}", retryableExceptionClasses.size());
        log.info("Loaded non-retryable exceptions: {}", nonRetryableExceptionClasses.size());
    }

    public TaskExecutionStatus classify(Throwable ex) {
        Throwable current = ex;
        int depth = 0;

        while (current != null && depth < MAX_CAUSE_DEPTH) {
            if (matchesAny(current, retryableExceptionClasses)) {
                return TaskExecutionStatus.FAILED_RETRYABLE;
            }

            if (matchesAny(current, nonRetryableExceptionClasses)) {
                return TaskExecutionStatus.FAILED_NON_RETRYABLE;
            }

            current = current.getCause();
            depth++;
        }

        if (depth >= MAX_CAUSE_DEPTH) {
            log.warn("Reached max cause depth ({}) for exception: {}", MAX_CAUSE_DEPTH, ex.getClass().getName());
        }

        return TaskExecutionStatus.FAILED_RETRYABLE;
    }

    private boolean matchesAny(Throwable ex, Set<Class<?>> exceptionClasses) {
        for (Class<?> exceptionClass : exceptionClasses) {
            if (exceptionClass.isInstance(ex)) {
                return true;
            }
        }
        return false;
    }

    private Set<Class<?>> loadClasses(List<String> classNames) {
        Set<Class<?>> classes = new HashSet<>();

        if (classNames == null) {
            return classes;
        }

        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                classes.add(clazz);
            } catch (ClassNotFoundException e) {
                log.error("Exception class not found in async task configuration: {}", className, e);
                throw new IllegalArgumentException("Exception class not found in async task configuration: " + className);
            }
        }

        return classes;
    }
}
