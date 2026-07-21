package dev.sorokin.domain;

import dev.sorokin.domain.type.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(name = "address")
    private String address;

    @Column(name = "client_estimate", nullable = false, precision = 10, scale = 2)
    private BigDecimal clientEstimate;

    @Column(name = "final_amount", precision = 10, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "authorized_amount", precision = 10, scale = 2)
    private BigDecimal authorizedAmount;

    @Column(name = "captured_amount", precision = 10, scale = 2)
    private BigDecimal capturedAmount;

    @Builder.Default
    @Column(name = "payment_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.NEW;

    @Column(name = "authorization_id")
    private UUID authorizationId;

    @Column(name = "failure_reason")
    private String failureReason;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public void markAuthorized(BigDecimal authorizedAmount, UUID authorizationId) {
        this.authorizationId = authorizationId;
        this.authorizedAmount = authorizedAmount;
        this.paymentStatus = PaymentStatus.AUTHORIZED;
    }

    public void markAwaitingCapture() {
        this.paymentStatus = PaymentStatus.AWAITING_CAPTURE;
    }

    public void markSucceedPaid(BigDecimal capturedAmount) {
        this.capturedAmount = capturedAmount;
        this.paymentStatus = PaymentStatus.SUCCEED_PAID;
    }

    public void markWithStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public void markFailed(PaymentStatus paymentStatus, String failureReason) {
        this.paymentStatus = paymentStatus;
        this.failureReason = failureReason;
    }

    public void markFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public static OrderEntity createNew(String address, BigDecimal clientEstimate) {
        return OrderEntity.builder()
                .address(address)
                .clientEstimate(clientEstimate)
                .paymentStatus(PaymentStatus.NEW)
                .build();
    }
}
