-- changeset add-version-and-locked-until-:005

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until TIMESTAMPTZ;

DROP INDEX IF EXISTS idx_tasks_status_next_attempt_updated_at;

CREATE INDEX IF NOT EXISTS idx_tasks_poll
    ON tasks (status, next_attempt_at, locked_until)
    WHERE status IN ('NEW', 'IN_PROGRESS', 'FAILED_RETRYABLE');

COMMENT ON COLUMN orders.version IS 'Версия строки для optimistic locking';
COMMENT ON COLUMN tasks.version IS 'Версия строки для optimistic locking';
COMMENT ON COLUMN tasks.locked_until IS 'До какого момента задача считается занятой исполнителем';
