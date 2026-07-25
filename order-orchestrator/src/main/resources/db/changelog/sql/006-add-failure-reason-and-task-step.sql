-- changeset add-failure-reason-and-task-step-:006

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS failure_reason TEXT;

COMMENT ON COLUMN orders.failure_reason IS 'Текстовая причина неуспешной оплаты для клиента и поддержки';

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS step TEXT;

COMMENT ON COLUMN tasks.step IS 'Последний завершённый или начатый шаг пайплайна: AUTH, REPRICE, CAPTURE';
