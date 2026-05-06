import http from 'k6/http';
import { check, sleep } from 'k6';

// 목록 조회로 받은 여러 기사 ID로 랜덤 조회
const articleIds = [
  'b26e0ef7-06c2-4c0d-abb0-953b06b1166b',
  '6b718972-3de3-4bdd-af65-8900f0741e42',
  '7bb841ba-d258-4afd-a1a9-b46c6da573cc',
  'a9ce7579-0dd9-4271-98eb-efedb21376f2',
  '5fcf5caf-8ea2-49d2-bd7e-86a1b2519782',
  '01aa9d11-cc30-4393-8905-486e40d8a157',
  'f5709549-7f2f-404c-b634-ae08963f417d',
  '61ad10aa-f026-4188-b4a7-2ac76f356158',
  '39ee7b52-8904-4937-a566-c67cf00b8ab4',
  '80a758af-0ca4-4573-9e4e-47734f8e2cd9',
];

export const options = {
  // 결과 출력 시 보여줄 지표 지정
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],

  // 실제 트래픽처럼 서서히 증가했다가 감소하는 시나리오
  stages: [
    { duration: '30s', target: 50 },
    { duration: '30s', target: 300 },
    { duration: '10s', target: 0 },
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
      'Monew-Request-User-ID': '57a2ae38-7e21-4217-a614-f3ce1b29d7fc',
      // 하드코딩 되어 있으므로 다른 컴퓨터에서 실행할 때는 유저를 새로 만들고 바꿔주어야 함
    },
  };

  // 랜덤으로 기사 ID 선택
  const randomId = articleIds[Math.floor(Math.random() * articleIds.length)];

  // API 요청
  const res = http.get(
      `http://localhost:8080/api/articles/${randomId}`,
      params
  );

  // 응답 검증
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1); // 1초 대기 후 다음 요청 (실제 사용자처럼 딜레이)
}