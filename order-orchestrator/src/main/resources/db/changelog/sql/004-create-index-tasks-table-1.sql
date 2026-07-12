-- changeset create-index-tasks-1-:004
DROP INDEX idx_tasks_status_next_attempt;

CREATE INDEX IF NOT EXISTS idx_tasks_status_next_attempt_updated_at ON tasks (status, next_attempt_at, updated_at) WHERE status IN ('NEW', 'IN_PROGRESS', 'FAILED_RETRYABLE');
CREATE INDEX IF NOT EXISTS idx_tasks_created_at ON tasks (created_at);
