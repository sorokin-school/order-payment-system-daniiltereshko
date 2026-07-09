package dev.sorokin.api;

import dev.sorokin.domain.type.PaymentStatus;
import java.math.BigDecimal;
import lombok.Builder;

import java.util.UUID;

@Builder
public record OrderDto(
        UUID orderId,
        String address,
        BigDecimal clientEstimate,
        BigDecimal finalAmount,
        BigDecimal capturedAmount,
        PaymentStatus paymentStatus
) { }
