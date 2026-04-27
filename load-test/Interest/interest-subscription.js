import http from 'k6/http';
import { check, sleep } from 'k6';

// 구독할 관심사 ID 목록
const interestIds = [
  'b3a21185-6bdb-4a3e-a012-d42c72514513',
  '35ffddd4-e650-45b8-9377-f2fbf7da06db',
  '9d197bc3-a7f2-426f-b1d3-e5cdeab1fb5a',
  '09872650-5764-490a-9dc2-b73ecce5fced',
  'f804bcf8-8668-44db-a3c9-ca3a1a218838',
];

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  stages: [
    { duration: '30s', target: 50 },
    { duration: '30s', target: 300 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const params = {
    headers: {
      'Monew-Request-User-ID': 'a9dd4279-a753-45ee-95d7-b1c8cb4fafb1',
    },
  };

  // 랜덤으로 관심사 ID 선택
  const randomId = interestIds[Math.floor(Math.random() * interestIds.length)];

  // POST 요청 (body 없음)
  const res = http.post(
      `http://localhost:8080/api/interests/${randomId}/subscriptions`,
      null,
      params
  );

  check(res, {
    'status is 201': (r) => r.status === 201,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);
}