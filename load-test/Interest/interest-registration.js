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
      'Content-Type': 'application/json',
      'Monew-Request-User-ID': 'a9dd4279-a753-45ee-95d7-b1c8cb4fafb1',
    },
  };

  // 랜덤 관심사 이름 생성 (유사도 검사 통과를 위해 충분히 다른 이름)
  const topics = ['스포츠', '경제', '정치', '문화', '과학', '기술', '의학', '환경', '교육', '여행'];
  const randomTopic = topics[Math.floor(Math.random() * topics.length)];
  const randomName = `${randomTopic}_${__VU}_${__ITER}_${Date.now()}`;
  const payload = JSON.stringify({
    name: randomName,
    keywords: ['키워드1', '키워드2'],
  });

  // POST 요청
  const res = http.post(
      'http://localhost:8080/api/interests',
      payload,
      params
  );

  // 응답 검증
  check(res, {
    'status is 201': (r) => r.status === 201,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);
}