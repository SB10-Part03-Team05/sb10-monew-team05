import http from 'k6/http';
import { check, sleep } from 'k6';

// 목록 조회로 받은 여러 기사 ID로 랜덤 조회
const articleIds = [
  '7d8a88b7-71cc-4752-b980-7417f6f742df',
  '440ba73c-1fd5-4ffc-a8b4-4a7ff162ccf0',
  '4b75eeee-91b3-4635-9aee-55353dca8dea',
  '5c87e953-48ca-4a43-8929-4c433481cd27',
  '98e0d0e6-9b67-499b-bb76-c5114e9b18ee',
];

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

  // 랜덤으로 기사 ID 선택
  const randomId = articleIds[Math.floor(Math.random() * articleIds.length)];

  // POST 요청 (body 없음, path variable로 기사 ID 전달)
  const res = http.post(
      `http://localhost:8080/api/articles/${randomId}/article-views`,
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