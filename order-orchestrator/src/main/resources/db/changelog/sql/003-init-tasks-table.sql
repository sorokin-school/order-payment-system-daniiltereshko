-- changeset init-tasks-:003

CREATE TYPE tasks_status_enum AS ENUM ('NEW', 'IN_PROGRESS', 'SUCCEEDED', 'FAILED_RETRYABLE', 'FAILED_NON_RETRYABLE');

CREATE TABLE IF NOT EXISTS tasks
(
    id                  UUID PRIMARY KEY DEFAULT uuidv7(),
    order_id            UUID REFERENCES orders(id) NOT NULL,
    status              tasks_status_enum NOT NULL DEFAULT 'NEW',
    attempts            INTEGER NOT NULL DEFAULT 0,
    next_attempt_at     TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_attempts_positive CHECK (attempts >= 0),
    CONSTRAINT chk_next_attempt_future CHECK (next_attempt_at > created_at OR next_attempt_at IS NULL)
);

CREATE INDEX IF NOT EXISTS idx_tasks_order_id ON tasks (order_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status_next_attempt ON tasks (status, next_attempt_at) WHERE status IN ('NEW', 'IN_PROGRESS', 'FAILED_RETRYABLE');

COMMENT ON TABLE tasks IS 'Задачи для обработки заказов';
COMMENT ON COLUMN tasks.id IS 'Уникальный идентификатор задачи';
COMMENT ON COLUMN tasks.order_id IS 'ID заказа, к которому привязана задача';
COMMENT ON COLUMN tasks.status IS 'Статус задачи';
COMMENT ON COLUMN tasks.attempts IS 'Количество попыток выполнения';
COMMENT ON COLUMN tasks.next_attempt_at IS 'Время следующей попытки выполнения';
COMMENT ON COLUMN tasks.created_at IS 'Время создания задачи';
COMMENT ON COLUMN tasks.updated_at IS 'Время последнего обновления задачи';