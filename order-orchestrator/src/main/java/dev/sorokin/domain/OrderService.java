package dev.sorokin.domain;

import dev.sorokin.api.OrderCreateRequestDto;
import dev.sorokin.async.task.service.TaskService;
import dev.sorokin.domain.type.PaymentStatus;
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
    public OrderEntity createOrder(
            OrderCreateRequestDto requestDto
    ) {
        var entity = OrderEntity.builder()
                .address(requestDto.address())
                .paymentStatus(PaymentStatus.NEW)
                .clientEstimate(requestDto.clientEstimate())
                .build();

        var savedOrder = orderRepository.save(entity);
        log.info("Order created with id={}, clientEstimate={}",
                savedOrder.getId(), savedOrder.getClientEstimate());

        taskService.createTaskForOrder(entity);
        return savedOrder;
    }

    @Transactional(readOnly = true)
    public Optional<OrderEntity> findOrder(UUID id) {
        return orderRepository.findById(id);
    }
}
