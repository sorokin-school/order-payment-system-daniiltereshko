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
@RequiredArgsConstructor
public class ErrorTypeClassifier {

    private final TaskAsyncProperties properties;

    private Set<Class<?>> retryableExceptionClasses = new HashSet<>();
    private Set<Class<?>> nonRetryableExceptionClasses = new HashSet<>();

    @PostConstruct
    public void init() {
        retryableExceptionClasses = loadClasses(properties.getRetyableExceptions());
        nonRetryableExceptionClasses = loadClasses(properties.getNonRetryableExceptions());

        log.info("Loaded retryable exceptions: {}", retryableExceptionClasses.size());
        log.info("Loaded non-retryable exceptions: {}", nonRetryableExceptionClasses.size());
    }

    public TaskExecutionStatus classify(Throwable ex) {
        Throwable current = ex;

        while (current != null) {
            if (matchesAny(current, retryableExceptionClasses)) {
                return TaskExecutionStatus.FAILED_RETRYABLE;
            }

            if (matchesAny(current, nonRetryableExceptionClasses)) {
                return TaskExecutionStatus.FAILED_NON_RETRYABLE;
            }

            current = current.getCause();
        }

        return TaskExecutionStatus.FAILED_NON_RETRYABLE;
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
            }
        }

        return classes;
    }
}
