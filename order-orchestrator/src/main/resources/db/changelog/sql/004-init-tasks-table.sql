-- changeset init-tasks-:004

CREATE TABLE IF NOT EXISTS tasks
(
    id                  UUID PRIMARY KEY DEFAULT uuidv7(),
    order_id            UUID NOT NULL REFERENCES orders(id),
    status              TEXT NOT NULL DEFAULT 'NEW',
    attempts            INTEGER NOT NULL DEFAULT 0,
    next_attempt_at     TIMESTAMPTZ,
    locked_until        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_attempts_positive CHECK (attempts >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_tasks_order_id ON tasks (order_id);

CREATE INDEX IF NOT EXISTS idx_tasks_poll
    ON tasks (status, next_attempt_at, locked_until)
    WHERE status IN ('NEW', 'IN_PROGRESS', 'FAILED_RETRYABLE');

COMMENT ON TABLE tasks IS 'Задачи для обработки заказов';
COMMENT ON COLUMN tasks.id IS 'Уникальный идентификатор задачи';
COMMENT ON COLUMN tasks.order_id IS 'ID заказа, к которому привязана задача';
COMMENT ON COLUMN tasks.status IS 'Статус задачи';
COMMENT ON COLUMN tasks.attempts IS 'Количество попыток выполнения';
COMMENT ON COLUMN tasks.next_attempt_at IS 'Время следующей попытки выполнения';
COMMENT ON COLUMN tasks.locked_until IS 'До какого момента задача считается занятой исполнителем';
COMMENT ON COLUMN tasks.created_at IS 'Время создания задачи';
COMMENT ON COLUMN tasks.updated_at IS 'Время последнего обновления задачи';
COMMENT ON COLUMN tasks.version IS 'Версия строки для optimistic locking';
