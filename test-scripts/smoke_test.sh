#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
CLIENT_ESTIMATE="${CLIENT_ESTIMATE:-500.00}"
POLL_ATTEMPTS="${POLL_ATTEMPTS:-120}"
POLL_INTERVAL_SEC="${POLL_INTERVAL_SEC:-0.5}"

TERMINAL_STATUSES=(
  "SUCCEED_PAID"
  "AUTHORIZATION_FAILED"
  "PRICE_CHANGED_FAILED"
  "CAPTURED_FAILED"
)

is_terminal_status() {
  local status="$1"
  for terminal in "${TERMINAL_STATUSES[@]}"; do
    if [[ "$status" == "$terminal" ]]; then
      return 0
    fi
  done
  return 1
}

echo "[SMOKE] Creating order..."
CREATE_JSON=$(curl -sS -X POST "$BASE_URL/order" \
  -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d "{\"address\":\"smoke-street\",\"clientEstimate\":${CLIENT_ESTIMATE}}")

ORDER_ID=$(echo "$CREATE_JSON" | jq -r '.orderId')

if [[ -z "$ORDER_ID" || "$ORDER_ID" == "null" ]]; then
  echo "[SMOKE][FAIL] create returned no orderId. Body: $CREATE_JSON" >&2
  exit 1
fi

echo "[SMOKE] Created orderId=$ORDER_ID. Polling paymentStatus..."
TERMINAL_STATE_REACHED=0
LAST_STATUS=""

for ((i = 1; i <= POLL_ATTEMPTS; i++)); do
  RESP=$(curl -sS -H 'Accept: application/json' "$BASE_URL/order/$ORDER_ID" || true)
  STATUS=$(echo "$RESP" | jq -r '.paymentStatus // empty')
  LAST_STATUS="$STATUS"

  if is_terminal_status "$STATUS"; then
    FINAL_AMOUNT=$(echo "$RESP" | jq -r '.finalAmount // empty')
    CAPTURED_AMOUNT=$(echo "$RESP" | jq -r '.capturedAmount // empty')
    echo "[SMOKE] Final paymentStatus: $STATUS"
    echo "[SMOKE] finalAmount=$FINAL_AMOUNT capturedAmount=$CAPTURED_AMOUNT"
    TERMINAL_STATE_REACHED=1
    break
  fi

  sleep "$POLL_INTERVAL_SEC"
done

if [[ $TERMINAL_STATE_REACHED -ne 1 ]]; then
  echo "[SMOKE][FAIL] Order did not reach terminal paymentStatus in time. Last status: ${LAST_STATUS:-unknown}" >&2
  exit 2
fi

echo "[SMOKE][OK]"
