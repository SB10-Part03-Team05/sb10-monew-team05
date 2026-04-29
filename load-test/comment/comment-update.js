import http from 'k6/http';
import { check, sleep } from 'k6';

const commentIds = [
  'cf69e3fe-f98f-4cda-b772-237ded3d2514',
  'fd5ff7fb-a7cb-4aad-ab57-b936d752f534',
  '37f57bed-d4c1-40cc-b0d0-8a621d08c1bb',
  'dfc97e1e-3527-4f67-99a2-2035d7e851bb',
  'dbe01eb8-a977-4fc7-83ed-d5fc30d15392',
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

  // 랜덤으로 댓글 ID 선택
  const randomCommentId = commentIds[Math.floor(Math.random() * commentIds.length)];

  const payload = JSON.stringify({
    content: `수정된 댓글입니다. ${__VU}_${__ITER}`,
  });

  // PATCH 요청
  const res = http.patch(
      `http://localhost:8080/api/comments/${randomCommentId}`,
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