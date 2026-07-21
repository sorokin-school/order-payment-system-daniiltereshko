package dev.sorokin.async.task.repository;

import dev.sorokin.async.task.entity.TaskEntity;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskJpaRepository extends JpaRepository<TaskEntity, UUID> {

    @Query(value = """
        SELECT t.id FROM tasks t
        WHERE t.status = 'NEW'
        OR (t.status = 'FAILED_RETRYABLE' AND t.next_attempt_at <= :now)
        OR (t.status = 'IN_PROGRESS' AND (t.locked_until IS NULL OR t.locked_until <= :now))
        ORDER BY t.created_at
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<UUID> lockTaskIdsForProcessing(
            @Param("now") OffsetDateTime now,
            @Param("limit") int limit
    );

    @Query(value = """
        SELECT t FROM TaskEntity t
        JOIN FETCH t.order
        WHERE t.id IN :ids
        ORDER BY t.createdAt
        """)
    List<TaskEntity> findAllByIdInWithOrder(@Param("ids") Collection<UUID> ids);
}
