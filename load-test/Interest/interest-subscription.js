import http from 'k6/http';
import { check, sleep } from 'k6';

// 구독할 관심사 ID 목록
const interestIds = [
  '09d5315d-6b26-4669-a2c2-069929c48223',
  '973203f1-1c9f-449b-9645-2d71f35db933',
  '868f9c13-4b1e-4a58-8d3c-337665a0225a',
  '28814481-4896-4245-8dc2-666bdd9ece57',
  'f812a78f-fc68-4adf-bf5d-88960588800e',
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
  },
};

export default function () {
  const params = {
    headers: {
      'Monew-Request-User-ID': '57a2ae38-7e21-4217-a614-f3ce1b29d7fc',
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
    'status is 201 or 409': (r) => r.status === 201 || r.status === 409,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);
}