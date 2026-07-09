package dev.sorokin.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    NEW("NEW"),
    AUTHORIZATION_FAILED("AUTHORIZATION_FAILED"),
    PRICE_CHANGED_FAILED("PRICE_CHANGED_FAILED"),
    SUCCEED_PAID("SUCCEED_PAID");

    private final String name;
}
