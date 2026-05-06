import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  // 실제 트래픽처럼 서서히 증가했다가 감소하는 시나리오
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

  // PATCH 요청
  const res = http.patch(
      'http://localhost:8080/api/notifications',
      null,
      params
  );

  // 응답 검증
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);
}