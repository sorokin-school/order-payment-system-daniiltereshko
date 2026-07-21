-- changeset add-columns-to-orders-table-:002

ALTER TABLE orders
ADD COLUMN client_estimate DECIMAL(10, 2) NOT NULL,
ADD COLUMN final_amount DECIMAL(10, 2),
ADD COLUMN authorized_amount DECIMAL(10, 2),
ADD COLUMN captured_amount DECIMAL(10, 2),
ADD COLUMN payment_status text NOT NULL DEFAULT 'NEW',
ADD COLUMN authorization_id uuid;

COMMENT ON COLUMN orders.client_estimate IS 'Оценка стоимости от клиента';
COMMENT ON COLUMN orders.final_amount IS 'Итоговая сумма заказа';
COMMENT ON COLUMN orders.authorized_amount IS 'Авторизованная сумма (заблокирована)';
COMMENT ON COLUMN orders.captured_amount IS 'Списанная сумма (фактически оплачена)';
COMMENT ON COLUMN orders.payment_status IS 'Статус оплаты';