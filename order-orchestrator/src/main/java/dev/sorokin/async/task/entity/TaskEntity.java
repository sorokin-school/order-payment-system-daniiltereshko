package dev.sorokin.async.task.entity;

import dev.sorokin.async.task.type.TaskStatus;
import dev.sorokin.async.task.type.TaskStep;
import dev.sorokin.domain.OrderEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "tasks")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TaskEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private OrderEntity order;

    @Builder.Default
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.NEW;

    @Builder.Default
    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "next_attempt_at")
    private OffsetDateTime nextAttemptAt;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "step")
    @Enumerated(EnumType.STRING)
    private TaskStep step;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    private TaskEntity(OrderEntity order) {
        this.order = order;
        this.status = TaskStatus.NEW;
        this.attempts = 0;
    }

    public static TaskEntity createForOrder(OrderEntity order) {
        return new TaskEntity(order);
    }

    public void markInProgress(OffsetDateTime lockedUntil) {
        this.status = TaskStatus.IN_PROGRESS;
        this.attempts = this.attempts + 1;
        this.lockedUntil = lockedUntil;
    }

    public void extendLock(OffsetDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public void advanceStep(TaskStep step) {
        this.step = step;
    }

    public void markSucceeded() {
        this.status = TaskStatus.SUCCEEDED;
        this.nextAttemptAt = null;
        this.lockedUntil = null;
    }

    public void markFailedRetryable(OffsetDateTime nextAttemptAt) {
        this.status = TaskStatus.FAILED_RETRYABLE;
        this.nextAttemptAt = nextAttemptAt;
        this.lockedUntil = null;
    }

    public void markFailedNonRetryable() {
        this.status = TaskStatus.FAILED_NON_RETRYABLE;
        this.nextAttemptAt = null;
        this.lockedUntil = null;
    }

}
