import http from 'k6/http';
import { sleep, check, fail } from 'k6';
import { Trend, Counter } from 'k6/metrics';

export const options = {
  scenarios: {
    create_and_poll: {
      executor: 'constant-vus',
      exec: 'default',
      vus: 60,
      duration: '10s',
    },
  },
};

const createLatency = new Trend('create_latency_ms');
const terminalLatency = new Trend('terminal_latency_ms');
const ordersCreated = new Counter('orders_created_total');

const baseUrl = __ENV.BASE || 'http://localhost:8080';
const clientEstimate = __ENV.CLIENT_ESTIMATE || '500.00';
const jsonHeaders = {
  'Content-Type': 'application/json',
  Accept: 'application/json',
};

const reqTimeout = __ENV.HTTP_TIMEOUT || '5s';
const terminalStatuses = new Set([
  'SUCCEED_PAID',
  'AUTHORIZATION_FAILED',
  'PRICE_CHANGED_FAILED',
  'CAPTURED_FAILED',
]);

function getFieldFromJson(jsonString, fieldName) {
  try {
    return jsonString.json(fieldName);
  } catch (_) {
    return null;
  }
}

export default function () {
  const createPayload = JSON.stringify({
    address: `student-street-${__VU}-${__ITER}`,
    clientEstimate: Number(clientEstimate),
  });

  const t0 = Date.now();
  const createResp = http.post(`${baseUrl}/order`, createPayload, {
    headers: jsonHeaders,
    timeout: reqTimeout,
  });
  createLatency.add(createResp.timings.duration);

  const createdOk = check(createResp, {
    'create 201': (r) => r.status === 201,
    'create JSON': (r) => (r.headers['Content-Type'] || '').includes('application/json'),
  });

  if (!createdOk) {
    fail(`Create failed: status=${createResp.status}, body=${createResp.body}`);
  }

  const orderId = getFieldFromJson(createResp, 'orderId');
  if (!orderId) {
    fail(`No orderId in response: body=${createResp.body}`);
  }

  ordersCreated.add(1);

  const maxPollIters = 15;
  let sleepMs = 1;
  let finalized = false;
  let lastStatus = null;

  for (let i = 0; i < maxPollIters; i++) {
    const pollResp = http.get(`${baseUrl}/order/${orderId}`, {
      headers: jsonHeaders,
      timeout: reqTimeout,
    });

    const ok = check(pollResp, {
      'poll 200/404': (r) => r.status === 200 || r.status === 404,
    });
    if (!ok || pollResp.status === 404) {
      sleep(sleepMs / 1000);
      sleepMs *= 2;
      continue;
    }

    lastStatus = getFieldFromJson(pollResp, 'paymentStatus');

    if (terminalStatuses.has(lastStatus)) {
      const t1 = Date.now();
      terminalLatency.add(t1 - t0, { paymentStatus: lastStatus });
      finalized = true;
      break;
    }

    sleep(sleepMs / 1000);
    sleepMs *= 2;
  }

  check(finalized, {
    'order reached terminal paymentStatus': (v) => v === true,
  });
}
