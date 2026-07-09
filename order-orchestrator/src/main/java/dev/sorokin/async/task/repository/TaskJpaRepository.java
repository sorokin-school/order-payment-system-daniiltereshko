package dev.sorokin.async.task.repository;

import dev.sorokin.async.task.entity.TaskEntity;
import dev.sorokin.domain.OrderEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskJpaRepository extends JpaRepository<TaskEntity, UUID> {
}
