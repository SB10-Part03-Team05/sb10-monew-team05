import http from 'k6/http';
import { check, sleep } from 'k6';

// 목록 조회로 받은 여러 기사 ID로 랜덤 조회
const articleIds = [
  '5c1678e9-8728-4115-a4d2-9cf6be2cc4ea',
  'fa9ae809-4547-4e3c-ac34-6dd0fb40a73f',
  '96dd09af-f96b-4425-a1f8-45e2ddef8623',
  'ceef100b-41ac-4c7f-af61-aaeca6a7a98b',
  '9c932093-eb6b-489d-b62c-602740cc8879',
  '1e682f4c-0235-47b1-961a-3168b5295dcc',
  '53c2faf3-a9d3-41da-b70b-6a25eb997746',
  '328103c1-dd90-4c10-8dbc-6d0fc42ae528',
  'ca4c0f78-f9dc-4b3f-a9f2-79fc41f046e1',
  '06df24dd-7335-4aa8-8ac8-d75352f22ec9',
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
      'Monew-Request-User-ID': 'a9dd4279-a753-45ee-95d7-b1c8cb4fafb1',
      // 하드코딩 되어 있으므로 다른 컴퓨터에서 실행할 때는 유저를 새로 만들고 바꿔주어야 함
    },
  };

  // 랜덤으로 기사 ID 선택
  const randomId = articleIds[Math.floor(Math.random() * articleIds.length)];

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