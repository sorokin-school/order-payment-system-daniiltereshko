package dev.sorokin.async.task.repository;

import dev.sorokin.async.task.entity.TaskEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskJpaRepository extends JpaRepository<TaskEntity, UUID> {

    @Query(value = """
        SELECT * FROM tasks t
        WHERE t.status = 'NEW'
        OR (t.status = 'FAILED_RETRYABLE' AND t.next_attempt_at <= NOW())
        OR (t.status = 'IN_PROGRESS' AND t.updated_at <= :inProgressTimeoutThreshold)
        ORDER BY t.created_at
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<TaskEntity> findTasksForProcessing(
            @Param("inProgressTimeoutThreshold") OffsetDateTime inProgressTimeoutThreshold,
            @Param("limit") int limit
    );

}
