import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  // 결과 출력 시 보여줄 지표 지정
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],

  // 실제 트래픽처럼 서서히 증가했다가 감소하는 시나리오
  stages: [
    { duration: '10s', target: 10 },  // 0~10초: 가상 유저 0명 -> 10명으로 증가
    { duration: '30s', target: 50 },  // 10~40초: 10명 -> 50명으로 증가
    { duration: '10s', target: 0 },   // 40~50초: 50명 -> 0명으로 감소
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'], // p95가 500ms 넘으면 테스트 실패, p99가 1000ms 넘으면 테스트 실패
    http_req_failed: ['rate<0.01'], // 에러율이 1% 넘으면 테스트 실패
  },
};

export default function () {
  // 헤더 설정 (로그인한 사용자 ID)
  const params = {
    headers: {
      'Monew-Request-User-ID': 'a9dd4279-a753-45ee-95d7-b1c8cb4fafb1',
      // 하드코딩 되어 있으므로 다른 컴퓨터에서 실행할 때는 유저를 새로 만들고 바꿔주어야 함
    },
  };

  // API 요청
  const res = http.get(
      'http://localhost:8080/api/articles?orderBy=publishDate&direction=DESC&limit=10',
      params
  );

  // 응답 검증
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1); // 1초 대기 후 다음 요청 (실제 사용자처럼 딜레이)
}