package dev.sorokin.async.task.service;

import dev.sorokin.async.config.properties.TaskAsyncProperties;
import dev.sorokin.async.task.entity.TaskEntity;
import dev.sorokin.async.task.repository.TaskJpaRepository;
import dev.sorokin.domain.OrderEntity;
import dev.sorokin.domain.OrderJpaRepository;
import dev.sorokin.domain.type.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskCompletionService {

    private final TaskJpaRepository taskRepository;
    private final OrderJpaRepository orderRepository;
    private final TaskAsyncProperties taskAsyncProperties;

    @Transactional
    public void completeSuccess(UUID taskId, UUID orderId, BigDecimal capturedAmount) {
        OrderEntity order = requireOrder(orderId);
        order.markSucceedPaid(capturedAmount);
        orderRepository.save(order);

        TaskEntity task = requireTask(taskId);
        task.markSucceeded();
        taskRepository.save(task);

        log.info("Task completed successfully: taskId={}, orderId={}, capturedAmount={}", taskId, orderId, capturedAmount);
    }

    @Transactional
    public void completeRetryable(UUID taskId) {
        TaskEntity task = requireTask(taskId);
        if (task.getAttempts() >= taskAsyncProperties.getMaxAttempts()) {
            log.warn("Task exceeded max attempts after retryable failure: taskId={}, attempts={}, maxAttempts={}",
                    taskId, task.getAttempts(), taskAsyncProperties.getMaxAttempts());
            task.markFailedNonRetryable();
        } else {
            OffsetDateTime nextAttemptAt = OffsetDateTime.now()
                    .plusSeconds(taskAsyncProperties.getRetryDelaySeconds());
            task.markFailedRetryable(nextAttemptAt);
        }
        taskRepository.save(task);
    }

    @Transactional
    public void completeNonRetryable(UUID taskId, UUID orderId, PaymentStatus paymentStatus) {
        completeNonRetryable(taskId, orderId, paymentStatus, null);
    }

    @Transactional
    public void completeNonRetryable(
            UUID taskId,
            UUID orderId,
            PaymentStatus paymentStatus,
            String failureReason
    ) {
        if (paymentStatus != null) {
            OrderEntity order = requireOrder(orderId);
            if (failureReason != null) {
                order.markFailed(paymentStatus, failureReason);
            } else {
                order.markWithStatus(paymentStatus);
            }
            orderRepository.save(order);
        }

        TaskEntity task = requireTask(taskId);
        task.markFailedNonRetryable();
        taskRepository.save(task);

        log.info("Task completed non-retryable: taskId={}, orderId={}, paymentStatus={}, failureReason={}",
                taskId, orderId, paymentStatus, failureReason);
    }

    private TaskEntity requireTask(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalStateException("Task not found: " + taskId));
    }

    private OrderEntity requireOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));
    }
}
