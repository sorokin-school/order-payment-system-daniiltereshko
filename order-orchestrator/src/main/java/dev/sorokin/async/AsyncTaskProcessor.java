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
import dev.sorokin.async.task.type.TaskExecutionStatus;
import dev.sorokin.client.PaymentClient;
import dev.sorokin.client.WarehouseClient;
import dev.sorokin.domain.OrderEntity;
import dev.sorokin.domain.OrderService;
import dev.sorokin.domain.type.PaymentStatus;
import dev.sorokin.util.BigDecimalUtils;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncTaskProcessor {

    private static final long STUB_CUSTOMER_ID = 1L;

    private final OrderService orderService;
    private final PaymentClient paymentClient;
    private final WarehouseClient warehouseClient;
    private final ThreadPoolTaskExecutor ioThreadPool;

    public TaskExecutionStatus process(TaskEntity task) {
        log.info("Start process task: taskId={}, orderId={}", task.getId(), task.getOrder().getId());
        OrderEntity order = task.getOrder();

        try {
            return runPaymentFlow(order);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Task failed retryable: taskId={}, orderId={}, paymentStatus={}, reason=thread_interrupted",
                    task.getId(), order.getId(), order.getPaymentStatus());
            return TaskExecutionStatus.FAILED_RETRYABLE;
        } catch (ExecutionException e) {
            log.error("Task execution error: taskId={}, orderId={}, paymentStatus={}, error={}",
                    task.getId(), order.getId(), order.getPaymentStatus(), e.getCause().getMessage(), e.getCause());
            throw new RuntimeException(e.getCause());
        }
    }

    private TaskExecutionStatus runPaymentFlow(OrderEntity order) throws InterruptedException, ExecutionException {
        CompletableFuture<AuthorizePaymentResponseDto> authorizeFuture =
                CompletableFuture.supplyAsync(() -> authorizePayment(order), ioThreadPool);
        CompletableFuture<CalculatePricingResponseDto> warehouseFuture =
                CompletableFuture.supplyAsync(() -> getWarehouseInfo(order), ioThreadPool);

        return authorizeFuture
                .thenCombine(warehouseFuture, (authResponse, warehouseResponse) ->
                        handlePayment(order, authResponse, warehouseResponse))
                .get();
    }

    private TaskExecutionStatus handlePayment(
            OrderEntity order,
            AuthorizePaymentResponseDto authResponse,
            CalculatePricingResponseDto warehouseResponse
    ) {
        if (AuthorizationStatus.DECLINED.equals(authResponse.status())) {
            log.error("Task failed non-retryable: orderId={}, paymentStatus={}, authMessage={}",
                    order.getId(), order.getPaymentStatus(), authResponse.message());
            return TaskExecutionStatus.FAILED_NON_RETRYABLE;
        }

        BigDecimal authorizedAmount = authResponse.authorizedAmount();
        BigDecimal finalAmount = warehouseResponse.finalAmount();

        if (BigDecimalUtils.isGreaterThan(finalAmount, authorizedAmount)) {
            return failWithPriceChanged(order, finalAmount, authorizedAmount);
        }

        log.info("Capture approved: orderId={}, finalAmount={}, authorizedAmount={}",
                order.getId(), finalAmount, authorizedAmount);
        return capturePayment(order, finalAmount);
    }

    private TaskExecutionStatus failWithPriceChanged(
            OrderEntity order,
            BigDecimal finalAmount,
            BigDecimal authorizedAmount
    ) {
        order.setPaymentStatus(PaymentStatus.PRICE_CHANGED_FAILED);
        orderService.saveOrder(order);

        log.error("Task failed non-retryable: orderId={}, paymentStatus={}, finalAmount={}, authorizedAmount={}",
                order.getId(), order.getPaymentStatus(), finalAmount, authorizedAmount);
        return TaskExecutionStatus.FAILED_NON_RETRYABLE;
    }

    private TaskExecutionStatus capturePayment(OrderEntity order, BigDecimal finalAmount) {
        CapturePaymentRequestDto request = new CapturePaymentRequestDto(finalAmount, STUB_CUSTOMER_ID);
        CapturePaymentResponseDto response = paymentClient.capturePayment(request);

        if (CaptureStatus.CAPTURED.equals(response.status())) {
            order.setCapturedAmount(response.capturedAmount());
            order.setPaymentStatus(PaymentStatus.SUCCEED_PAID);
            orderService.saveOrder(order);
            log.info("Capture succeeded: orderId={}, captureStatus={}, capturedAmount={}, paymentStatus={}",
                    order.getId(), response.status(), response.capturedAmount(), order.getPaymentStatus());
            return TaskExecutionStatus.SUCCEEDED;
        }

        order.setPaymentStatus(PaymentStatus.CAPTURED_FAILED);
        orderService.saveOrder(order);
        log.error("Task failed non-retryable: orderId={}, paymentStatus={}, captureMessage={}",
                order.getId(), order.getPaymentStatus(), response.message());
        return TaskExecutionStatus.FAILED_NON_RETRYABLE;
    }

    private CalculatePricingResponseDto getWarehouseInfo(OrderEntity order) {
        CalculatePricingRequestDto request = new CalculatePricingRequestDto(order.getId());
        CalculatePricingResponseDto response = warehouseClient.calculatePricing(request);

        order.setFinalAmount(response.finalAmount());
        orderService.saveOrder(order);
        log.info("Warehouse repricing completed: orderId={}, finalAmount={}, reason={}",
                order.getId(), response.finalAmount(), response.reason());
        return response;
    }

    private AuthorizePaymentResponseDto authorizePayment(OrderEntity order) {
        AuthorizePaymentRequestDto request = new AuthorizePaymentRequestDto(STUB_CUSTOMER_ID, order.getClientEstimate());
        AuthorizePaymentResponseDto response = paymentClient.authorizePayment(request);

        updateOrderAfterAuthorization(order, response);
        orderService.saveOrder(order);

        boolean authorized = AuthorizationStatus.AUTHORIZED.equals(response.status());
        if (authorized) {
            log.info("Authorization succeeded: orderId={}, authStatus={}, authorizedAmount={}, paymentStatus={}, message={}",
                    order.getId(), response.status(), response.authorizedAmount(), order.getPaymentStatus(), response.message());
        } else {
            log.warn("Authorization failed: orderId={}, authStatus={}, paymentStatus={}, reasonCode={}, message={}",
                    order.getId(), response.status(), order.getPaymentStatus(), response.status(), response.message());
        }

        return response;
    }

    private void updateOrderAfterAuthorization(OrderEntity order, AuthorizePaymentResponseDto response) {
        if (AuthorizationStatus.AUTHORIZED.equals(response.status())) {
            order.setAuthorizedAmount(response.authorizedAmount());
        } else {
            order.setPaymentStatus(PaymentStatus.AUTHORIZATION_FAILED);
        }
    }
}
