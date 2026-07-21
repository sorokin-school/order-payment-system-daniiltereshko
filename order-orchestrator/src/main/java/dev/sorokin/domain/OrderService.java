package dev.sorokin.domain;

import dev.sorokin.api.OrderCreateRequestDto;
import dev.sorokin.async.task.service.TaskService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final TaskService taskService;
    private final OrderJpaRepository orderRepository;

    @Transactional
    public void markAuthorized(UUID orderId, BigDecimal authorizedAmount, UUID authorizationId) {
        OrderEntity order = requireOrder(orderId);
        order.markAuthorized(authorizedAmount, authorizationId);
        orderRepository.save(order);
    }

    @Transactional
    public void markAwaitingCapture(UUID orderId) {
        OrderEntity order = requireOrder(orderId);
        order.markAwaitingCapture();
        orderRepository.save(order);
    }

    @Transactional
    public void markFinalAmount(UUID orderId, BigDecimal finalAmount) {
        OrderEntity order = requireOrder(orderId);
        order.markFinalAmount(finalAmount);
        orderRepository.save(order);
    }

    @Transactional
    public OrderEntity createOrder(OrderCreateRequestDto requestDto) {
        var entity = OrderEntity.createNew(requestDto.getAddress(), requestDto.getClientEstimate());

        var savedOrder = orderRepository.save(entity);
        log.info("Order created with id={}, clientEstimate={}",
                savedOrder.getId(), savedOrder.getClientEstimate());

        taskService.createTaskForOrder(savedOrder);
        return savedOrder;
    }

    @Transactional(readOnly = true)
    public Optional<OrderEntity> findOrder(UUID id) {
        return orderRepository.findById(id);
    }

    private OrderEntity requireOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));
    }
}
