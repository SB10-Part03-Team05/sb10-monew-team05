import http from 'k6/http';
import { check, sleep } from 'k6';

const interestIds = [
  'b3a21185-6bdb-4a3e-a012-d42c72514513',
  '35ffddd4-e650-45b8-9377-f2fbf7da06db',
  '9d197bc3-a7f2-426f-b1d3-e5cdeab1fb5a',
  '09872650-5764-490a-9dc2-b73ecce5fced',
  'f804bcf8-8668-44db-a3c9-ca3a1a218838',
  '2df483af-4212-41c5-8db4-67fbecd9245a',
  '5f09b68a-885b-4a4b-82a3-b61fdc0ee728',
  'a6e6cd76-0434-4e29-8301-ef41adfa0f15',
  '3feafa18-04c4-47ee-b326-d99c7c2fd127',
  '92b4044e-bcaf-4bd5-baf2-9c0faa859221',
  '4dbab393-d105-4ab9-a731-26f058fa9cce',
  '962ae688-53c1-4562-bdc7-5bda4025626f',
  '84406a95-043b-47ee-8ec6-89856d06200d',
  '3ae7a98f-966c-40b5-8b69-44456d3ff5d5',
  '9c44f9e7-39c2-4779-9604-e16b625c6b18',
  'a1845f48-bd06-4df4-b576-55ef32a0abaf',
  'a0ea816f-bf44-405d-8523-f8446a17de34',
  'c0dd5242-75d3-4a05-9cb7-86ffc2b84460',
  'da145d86-4e10-4299-be7e-0d2bd9988a7b',
  '4e37012d-51c2-4717-95fa-ae844c32efda',
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
      'Content-Type': 'application/json',
      'Monew-Request-User-ID': 'a9dd4279-a753-45ee-95d7-b1c8cb4fafb1',
    },
  };

  // 랜덤으로 관심사 ID 선택
  const randomId = interestIds[Math.floor(Math.random() * interestIds.length)];

  const payload = JSON.stringify({
    keywords: [`키워드_${__VU}_${__ITER}`],
  });

  // PATCH 요청
  const res = http.patch(
      `http://localhost:8080/api/interests/${randomId}`,
      payload,
      params
  );

  // 응답 검증
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);
}