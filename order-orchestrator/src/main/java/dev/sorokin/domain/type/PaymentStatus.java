package dev.sorokin.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    NEW("NEW"),
    AUTHORIZED("AUTHORIZED "),
    AUTHORIZATION_FAILED("AUTHORIZATION_FAILED"),
    PRICE_CHANGED_FAILED("PRICE_CHANGED_FAILED"),
    CAPTURED_FAILED("CAPTURED_FAILED"),
    AWAITING_CAPTURE("AWAITING_CAPTURE"),
    SUCCEED_PAID("SUCCEED_PAID");

    private final String name;
}
