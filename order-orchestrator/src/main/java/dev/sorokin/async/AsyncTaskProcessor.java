package dev.sorokin.async;

import dev.sorokin.api.payment.AuthorizationStatus;
import dev.sorokin.api.payment.AuthorizePaymentRequestDto;
import dev.sorokin.api.payment.AuthorizePaymentResponseDto;
import dev.sorokin.api.payment.CapturePaymentRequestDto;
import dev.sorokin.api.payment.CapturePaymentResponseDto;
import dev.sorokin.api.payment.CaptureStatus;
import dev.sorokin.api.warehouse.CalculatePricingRequestDto;
import dev.sorokin.api.warehouse.CalculatePricingResponseDto;
import dev.sorokin.async.task.entity.TaskEntity;
import dev.sorokin.async.task.service.TaskCompletionService;
import dev.sorokin.async.task.service.TaskService;
import dev.sorokin.async.task.type.TaskStep;
import dev.sorokin.client.PaymentClient;
import dev.sorokin.client.WarehouseClient;
import dev.sorokin.domain.OrderEntity;
import dev.sorokin.domain.OrderService;
import dev.sorokin.domain.type.PaymentStatus;
import dev.sorokin.util.BigDecimalUtils;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncTaskProcessor {

    private static final long STUB_CUSTOMER_ID = 1L;

    private final TaskService taskService;
    private final TaskCompletionService taskCompletionService;
    private final OrderService orderService;
    private final PaymentClient paymentClient;
    private final WarehouseClient warehouseClient;

    public void process(TaskEntity task) {
        UUID taskId = task.getId();
        UUID orderId = task.getOrder().getId();
        log.info("Start process task: taskId={}, orderId={}", taskId, orderId);

        try {
            taskService.extendLock(taskId);
            runPaymentFlow(taskId, orderId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Task failed retryable: taskId={}, orderId={}, reason=thread_interrupted", taskId, orderId);
            taskCompletionService.completeRetryable(taskId);
        } catch (ExecutionException e) {
            log.error("Task execution error: taskId={}, orderId={}, error={}",
                    taskId, orderId, e.getCause().getMessage(), e.getCause());
            throw new RuntimeException(e.getCause());
        }
    }

    private void runPaymentFlow(UUID taskId, UUID orderId) throws InterruptedException, ExecutionException {
        OrderEntity order = requireOrder(orderId);

        AuthorizationStatus authStatus = authorizePayment(taskId, order);
        if (AuthorizationStatus.DECLINED.equals(authStatus)) {
            log.error("Task failed non-retryable: orderId={}, paymentStatus=AUTHORIZATION_FAILED", orderId);
            return;
        }

        taskService.extendLock(taskId);
        CalculatePricingResponseDto warehouseResponse = recalculatePrice(taskId, order);

        handlePayment(taskId, orderId, warehouseResponse);
    }

    private void handlePayment(
            UUID taskId,
            UUID orderId,
            CalculatePricingResponseDto warehouseResponse
    ) {
        OrderEntity order = requireOrder(orderId);
        BigDecimal authorizedAmount = order.getAuthorizedAmount();
        BigDecimal finalAmount = warehouseResponse.finalAmount();

        if (BigDecimalUtils.isGreaterThan(finalAmount, authorizedAmount)) {
            String failureReason = warehouseResponse.reason() != null && !warehouseResponse.reason().isBlank()
                    ? warehouseResponse.reason()
                    : "price has changed from %s to %s".formatted(authorizedAmount, finalAmount);
            log.error("Task failed non-retryable: orderId={}, finalAmount={}, authorizedAmount={}, reason={}",
                    orderId, finalAmount, authorizedAmount, failureReason);
            taskCompletionService.completeNonRetryable(
                    taskId, orderId, PaymentStatus.PRICE_CHANGED_FAILED, failureReason);
            return;
        }

        log.info("Capture approved: orderId={}, finalAmount={}, authorizedAmount={}",
                orderId, finalAmount, authorizedAmount);
        capturePayment(taskId, orderId, finalAmount);
    }

    private void capturePayment(UUID taskId, UUID orderId, BigDecimal finalAmount) {
        OrderEntity order = requireOrder(orderId);

        if (order.getCapturedAmount() != null && order.getCapturedAmount().compareTo(BigDecimal.ZERO) > 0) {
            log.warn("Order has already been captured. Captured amount: {}", order.getCapturedAmount());
            taskCompletionService.completeSuccess(taskId, orderId, order.getCapturedAmount());
            return;
        }

        TaskEntity task = taskService.findTask(taskId);
        if (TaskStep.CAPTURE.equals(task.getStep())
                || PaymentStatus.AWAITING_CAPTURE.equals(order.getPaymentStatus())) {
            String failureReason = "Capture was already initiated but outcome is unknown; manual reconciliation required";
            log.error("Skipping repeat capture: taskId={}, orderId={}", taskId, orderId);
            taskCompletionService.completeNonRetryable(
                    taskId, orderId, PaymentStatus.CAPTURED_FAILED, failureReason);
            return;
        }

        taskService.markCaptureInProgress(taskId, orderId);

        taskService.extendLock(taskId);
        order = requireOrder(orderId);
        CapturePaymentRequestDto request = new CapturePaymentRequestDto(
                finalAmount, STUB_CUSTOMER_ID, order.getAuthorizationId());
        CapturePaymentResponseDto response = paymentClient.capturePayment(request);

        if (CaptureStatus.FAILED.equals(response.status())) {
            String failureReason = response.message() != null ? response.message() : "Capture failed";
            log.error("Task failed non-retryable: orderId={}, captureMessage={}", orderId, failureReason);
            taskCompletionService.completeNonRetryable(
                    taskId, orderId, PaymentStatus.CAPTURED_FAILED, failureReason);
            return;
        }

        log.info("Capture succeeded: orderId={}, captureStatus={}, capturedAmount={}",
                orderId, response.status(), response.capturedAmount());
        taskCompletionService.completeSuccess(taskId, orderId, response.capturedAmount());
    }

    private CalculatePricingResponseDto recalculatePrice(UUID taskId, OrderEntity order) {
        if (order.getFinalAmount() != null) {
            log.info("Skipping warehouse repricing, final amount already set: orderId={}, finalAmount={}",
                    order.getId(), order.getFinalAmount());
            taskService.advanceStep(taskId, TaskStep.REPRICE);
            return new CalculatePricingResponseDto(order.getId(), order.getFinalAmount(), null);
        }

        taskService.extendLock(taskId);
        CalculatePricingRequestDto request = new CalculatePricingRequestDto(order.getId());
        CalculatePricingResponseDto response = warehouseClient.calculatePricing(request);

        orderService.markFinalAmount(order.getId(), response.finalAmount());
        taskService.advanceStep(taskId, TaskStep.REPRICE);

        log.info("Warehouse repricing completed: orderId={}, finalAmount={}, reason={}",
                order.getId(), response.finalAmount(), response.reason());
        return response;
    }

    private AuthorizationStatus authorizePayment(UUID taskId, OrderEntity order) {
        if (order.getClientEstimate() == null) {
            throw new IllegalArgumentException("Client estimate amount cannot be null");
        }
        if (order.getAuthorizedAmount() != null
                && order.getAuthorizedAmount().compareTo(BigDecimal.ZERO) > 0
                && order.getAuthorizationId() != null) {
            log.warn("Order has already been authorized. Authorized amount: {}", order.getAuthorizedAmount());
            taskService.advanceStep(taskId, TaskStep.AUTH);
            return AuthorizationStatus.AUTHORIZED;
        }

        taskService.extendLock(taskId);
        AuthorizePaymentRequestDto request = new AuthorizePaymentRequestDto(STUB_CUSTOMER_ID, order.getClientEstimate());
        AuthorizePaymentResponseDto response = paymentClient.authorizePayment(request);

        if (AuthorizationStatus.AUTHORIZED.equals(response.status())) {
            orderService.markAuthorized(order.getId(), response.authorizedAmount(), response.authorizationId());
            taskService.advanceStep(taskId, TaskStep.AUTH);
            log.info("Authorization succeeded: orderId={}, authStatus={}, authorizedAmount={}, authorizationId={}, message={}",
                    order.getId(), response.status(), response.authorizedAmount(), response.authorizationId(),
                    response.message());
            return AuthorizationStatus.AUTHORIZED;
        }

        String failureReason = response.message() != null ? response.message() : "Authorization declined";
        log.warn("Authorization failed: orderId={}, authStatus={}, message={}",
                order.getId(), response.status(), failureReason);
        taskCompletionService.completeNonRetryable(
                taskId, order.getId(), PaymentStatus.AUTHORIZATION_FAILED, failureReason);
        return AuthorizationStatus.DECLINED;
    }

    private OrderEntity requireOrder(UUID orderId) {
        return orderService.findOrder(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));
    }
}
