# 📰 모뉴 (Monew)

[![codecov](https://codecov.io/github/SB10-Part03-Team05/sb10-monew-team05/graph/badge.svg)](https://codecov.io/github/SB10-Part03-Team05/sb10-monew-team05)

> 여러 뉴스 API를 통합하여 사용자에게 맞춤형 뉴스를 제공하고, 의견을 나눌 수 있는 소셜 기능을 갖춘 서비스

📅 프로젝트 기간: 2026.04.14 ~ 2026.05.08  
📎 팀 협업 문서: [Notion](https://www.notion.so/jungh20000/SB10-Monew-Team05-342f59816c0280948b6ac9c8f80492eb?source=copy_link)

---

## 👥 팀원 구성

| 이름 | GitHub | 역할 | 담당                                                    |
|------|--------|------|-------------------------------------------------------|
| 박정현 | [@JungH200000](https://github.com/JungH200000) | Leader / Backend / CI·CD | 뉴스 기사 관리 (삭제, 목록 조회, 기사 뷰 등록, 백업 및 복구, CI/CD, AWS 관리) |
| 박나경 | [@parkngg](https://github.com/parkngg) | Backend | 댓글 관리, 알림 관리                                          |
| 박린 | [@boolynn17](https://github.com/boolynn17) | Backend | 관심사 관리, 부하 테스트                                        |
| 박성국 | [@PSG-00](https://github.com/PSG-00) | Backend | 뉴스 기사 관리 (엔티티 설계, API 수집)                             |
| 이규빈 | [@plzslp](https://github.com/plzslp) | Backend | 사용자 관리, 활동 내역 관리, 커스텀 메트릭 관리                          |

---

## 🛠 기술 스택

### Backend
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=flat-square&logo=spring&logoColor=white)
![QueryDSL](https://img.shields.io/badge/QueryDSL-4479A1?style=flat-square)
![MapStruct](https://img.shields.io/badge/MapStruct-FF6B6B?style=flat-square)
![WebClient](https://img.shields.io/badge/WebClient-6DB33F?style=flat-square)

### Database
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=flat-square&logo=mongodb&logoColor=white)

### Infra
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)
![AWS](https://img.shields.io/badge/AWS-232F3E?style=flat-square&logo=amazonwebservices&logoColor=white)

### 협업 도구
![Git](https://img.shields.io/badge/Git-F05032?style=flat-square&logo=git&logoColor=white)
![GitHub](https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=github&logoColor=white)
![Discord](https://img.shields.io/badge/Discord-5865F2?style=flat-square&logo=discord&logoColor=white)
![Notion](https://img.shields.io/badge/Notion-000000?style=flat-square&logo=notion&logoColor=white)

### 코드 품질
![CodeRabbit](https://img.shields.io/badge/CodeRabbit-FF6B35?style=flat-square)
![Codecov](https://img.shields.io/badge/Codecov-F01F7A?style=flat-square&logo=codecov&logoColor=white)
![k6](https://img.shields.io/badge/k6-7D64FF?style=flat-square&logo=k6&logoColor=white)

---
## 📚 팀원별 구현 기능 상세
### 👤 박정현
- 뉴스 기사 조회 API
  - 뉴스 기사 단건 조회, 출처 목록 조회, 조건 기반 뉴스 기사 목록 조회 구현
    - QueryDSL을 활용한 커서 페이지네이션 구현
- 뉴스 기사 삭제 및 뷰 등록 API
  - 뉴스 기사 논리/물리 삭제 API 구현
    - 사용자별 뉴스 기사 조회 이력 저장 API 구현
    - 중복 조회 방지를 위해 UNIQUE 제약 조건 및 동시 요청 예외 처리 적용
- 뉴스 기사 백업/복구 기능
  - Spring Batch 기반 뉴스 기사 백업 배치 및 스케줄러 구현
    - 뉴스 기사 데이터를 날짜별 JSON 파일로 변환해 AWS S3에 백업
    - S3 백업 파일과 DB 데이터를 비교해 누락된 기사만 복구하는 로직 구현
  - 복구 시 뉴스 기사와 관심사 매핑 정보까지 함께 복원
- CI/CD 파이프라인 구축
  - GitHub Actions 기반 테스트 및 배포 workflow 구현
    - PR/push 시 JaCoCo, Codecov 연동
    - Docker 이미지 build 후 ECR push 및 ECS Service 자동 배포 구현
- AWS 및 운영 환경 관리
  - S3, ECR, ECS, RDS 연동 배포 환경 설정
### 👤 박나경
- 댓글 기능 구현
  - 댓글 등록, 수정, 삭제, 목록 조회, 좋아요 등록 및 취소 API 구현 
  - 기사별 댓글 목록 QueryDSL 커서 기반 페이지네이션 구현
- 알림 기능 구현 
  - 이벤트 기반 비동기 알림 시스템 구축 
  - 알림 목록 조회, 단건 확인, 전체 확인 API 구현 
  - 미확인 알림 목록 QueryDSL 커서 기반 페이지네이션 구현 
  - JPA 벌크 연산(@Modifying)을 활용한 다건 알림 읽음 처리 및 삭제 성능 최적화 
  - Spring Scheduler를 활용한 주기적인 알림 물리 삭제 배치 작업 구현
### 👤 박린
- 관심사 기능 구현 
  - 관심사 등록/수정/삭제/목록 조회/구독/구독 취소
  - Levenshtein Distance 기반 유사도 검사 (80% 이상 유사 시 등록 불가)
  - 비관적 락(Pessimistic Lock)을 통한 동시성 제어
  - 커서 기반 페이지네이션

- k6 부하테스트 진행 
  - 300명 동시 접속 기준 성능 측정
  - 병목 지점 분석 및 개선
    - 뉴스 기사 목록 조회 캐싱 적용 (avg 268ms → 2.97ms, 약 90배 개선)
### 👤 박성국
- 프로젝트 초기 설정 
  - Docker 환경 설정
  - 프로필 설정
  - 전역 예외 처리
  - 로그 설정 및 백업
    - 주기적으로 S3에 로그 파일 백업
- 뉴스 기사 수집 
  - 여러 RSS에 대한 뉴스 기사 수집 
    - 호환성을 위한 기능 추가 
      - XML 스크랩 시 인코딩 
        - Jsoup를 통한 데이터 전처리
    - 네이버 뉴스 검색 API를 통한 키워드 별 기사 수집
      - 중복 제거 된 키워드 순회하면서 검색
- 크롤링 및 LLM 요약 생성 
  - 요약을 제공하지 않는 소스에 대한 요약 생성 
    - XML에서 제공된 링크에서 기사 크롤링
      - 이미 크롤링 한 기사라면 인메모리 캐싱으로 크롤링 X
        - LLM을 이용한 뉴스 기사 요약 생성
### 👤 이규빈
- 사용자 API 
  - 사용자 생성, 삭제, 수정, 로그인 로직 구현
- 활동 내역 API 
  - MongoDB를 활용한 사용자 활동 내역 조회 로직 구현
  - MongoDB 호출 시 응답 속도 개선을 위해 로컬 캐싱 적용
- 커스텀 메트릭
  - 커스텀 메트릭 엔드포인트 추가 
  - Prometheus, Grafana를 활용한 커스텀 대시보드 구축


---
## 📁  디렉토리 구조
<details>
<summary> 클릭해서 열기</summary>

```text
.
├── Dockerfile
├── README.md
├── build.gradle
├── docker-compose.yaml
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
├── gradlew.bat
├── load-test
│   ├── Interest
│   │   ├── Interest-update.js
│   │   ├── interest-list-load-test.js
│   │   ├── interest-registration.js
│   │   └── interest-subscription.js
│   ├── article
│   │   ├── article-detail.js
│   │   ├── article-list.js
│   │   ├── article-restore.js
│   │   ├── article-source.js
│   │   └── article-view.js
│   ├── comment
│   │   ├── comment-like.js
│   │   ├── comment-list.js
│   │   ├── comment-registration.js
│   │   └── comment-update.js
│   ├── notification
│   │   ├── notification-confirm-all.js
│   │   ├── notification-confirm.js
│   │   └── notification-list.js
│   ├── results
│   │   ├── article
│   │   │   ├── article-detail.md
│   │   │   ├── article-list.md
│   │   │   ├── article-restore.md
│   │   │   ├── article-source.md
│   │   │   └── article-view.md
│   │   ├── comment
│   │   │   ├── comment-like.md
│   │   │   ├── comment-list.md
│   │   │   ├── comment-registration.md
│   │   │   └── comment-update.md
│   │   ├── interest
│   │   │   ├── interest-list.md
│   │   │   ├── interest-registration.md
│   │   │   ├── interest-subscription.md
│   │   │   └── interest-update.md
│   │   ├── notification
│   │   │   ├── notification-confirm-all.md
│   │   │   ├── notification-confirm.md
│   │   │   └── notification-list.md
│   │   ├── user
│   │   │   ├── user-login.md
│   │   │   ├── user-registration.md
│   │   │   └── user-update.md
│   │   └── useractivity
│   │       └── useractivity-list.md
│   ├── user
│   │   ├── user-login.js
│   │   ├── user-registration.js
│   │   └── user-update.js
│   └── useractivity
│       └── useractivity-list.js
├── prometheus.yml
├── settings.gradle
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── codeit
    │   │           └── monew
    │   │               ├── MonewApplication.java
    │   │               ├── domain
    │   │               │   ├── article
    │   │               │   │   ├── ArticleSource.java
    │   │               │   │   ├── controller
    │   │               │   │   │   ├── AdminArticleController.java
    │   │               │   │   │   └── ArticleController.java
    │   │               │   │   ├── dto
    │   │               │   │   │   ├── backup
    │   │               │   │   │   │   └── ArticleBackupDto.java
    │   │               │   │   │   ├── request
    │   │               │   │   │   │   └── ArticleSearchRequest.java
    │   │               │   │   │   └── response
    │   │               │   │   │       ├── ArticleDto.java
    │   │               │   │   │       ├── ArticleRestoreResultDto.java
    │   │               │   │   │       ├── ArticleScrapeBatchRunResponse.java
    │   │               │   │   │       ├── ArticleViewDto.java
    │   │               │   │   │       └── CursorPageResponseArticleDto.java
    │   │               │   │   ├── entity
    │   │               │   │   │   ├── Article.java
    │   │               │   │   │   ├── ArticleInterest.java
    │   │               │   │   │   ├── ArticleInterestId.java
    │   │               │   │   │   ├── ArticleViewHistory.java
    │   │               │   │   │   └── type
    │   │               │   │   │       ├── ArticleDirection.java
    │   │               │   │   │       └── ArticleOrderBy.java
    │   │               │   │   ├── mapper
    │   │               │   │   │   ├── ArticleBackupMapper.java
    │   │               │   │   │   ├── ArticleMapper.java
    │   │               │   │   │   └── ArticleViewMapper.java
    │   │               │   │   ├── repository
    │   │               │   │   │   ├── ArticleInterestRepository.java
    │   │               │   │   │   ├── ArticleQueryRepository.java
    │   │               │   │   │   ├── ArticleRepository.java
    │   │               │   │   │   ├── ArticleViewHistoryRepository.java
    │   │               │   │   │   └── impl
    │   │               │   │   │       └── ArticleQueryRepositoryImpl.java
    │   │               │   │   ├── scheduler
    │   │               │   │   │   ├── ArticleScrapeBatchConfig.java
    │   │               │   │   │   ├── ArticleScrapeBatchNotificationPublisher.java
    │   │               │   │   │   ├── ArticleScrapeNotificationTasklet.java
    │   │               │   │   │   ├── ArticleScrapeResult.java
    │   │               │   │   │   ├── ArticleScrapeResultExecutionContextManager.java
    │   │               │   │   │   ├── ArticleScrapeScheduler.java
    │   │               │   │   │   ├── BatchCircuitBreaker.java
    │   │               │   │   │   ├── NaverArticleBatchJob.java
    │   │               │   │   │   ├── NaverArticleScrapeTasklet.java
    │   │               │   │   │   ├── RssArticleBatchJob.java
    │   │               │   │   │   ├── RssArticleScrapeTasklet.java
    │   │               │   │   │   └── backup
    │   │               │   │   │       ├── ArticleBackupBatchConfig.java
    │   │               │   │   │       ├── ArticleBackupBatchRunner.java
    │   │               │   │   │       ├── ArticleBackupJob.java
    │   │               │   │   │       └── ArticleBackupScheduler.java
    │   │               │   │   └── service
    │   │               │   │       ├── ArticleBackupService.java
    │   │               │   │       ├── ArticleRestoreService.java
    │   │               │   │       ├── ArticleScrapeBatchRunner.java
    │   │               │   │       ├── ArticleScrapePersistenceService.java
    │   │               │   │       ├── ArticleScrapeService.java
    │   │               │   │       └── ArticleService.java
    │   │               │   ├── comment
    │   │               │   │   ├── controller
    │   │               │   │   │   ├── CommentController.java
    │   │               │   │   │   └── CommentLikeController.java
    │   │               │   │   ├── dto
    │   │               │   │   │   ├── CommentCursorRequest.java
    │   │               │   │   │   ├── CommentDto.java
    │   │               │   │   │   ├── CommentLikeDto.java
    │   │               │   │   │   ├── CommentRegisterRequest.java
    │   │               │   │   │   ├── CommentUpdateRequest.java
    │   │               │   │   │   └── CursorPageResponseCommentDto.java
    │   │               │   │   ├── entity
    │   │               │   │   │   ├── Comment.java
    │   │               │   │   │   └── CommentLike.java
    │   │               │   │   ├── mapper
    │   │               │   │   │   └── CommentMapper.java
    │   │               │   │   ├── repository
    │   │               │   │   │   ├── CommentLikeRepository.java
    │   │               │   │   │   ├── CommentQueryRepository.java
    │   │               │   │   │   ├── CommentRepository.java
    │   │               │   │   │   └── impl
    │   │               │   │   │       └── CommentQueryRepositoryImpl.java
    │   │               │   │   └── service
    │   │               │   │       ├── CommentLikeService.java
    │   │               │   │       └── CommentService.java
    │   │               │   ├── interest
    │   │               │   │   ├── controller
    │   │               │   │   │   └── InterestController.java
    │   │               │   │   ├── dto
    │   │               │   │   │   ├── request
    │   │               │   │   │   │   ├── InterestRegisterRequest.java
    │   │               │   │   │   │   └── InterestUpdateRequest.java
    │   │               │   │   │   └── response
    │   │               │   │   │       ├── CursorPageResponseInterestDto.java
    │   │               │   │   │       ├── InterestDto.java
    │   │               │   │   │       └── SubscriptionDto.java
    │   │               │   │   ├── entity
    │   │               │   │   │   ├── Interest.java
    │   │               │   │   │   ├── Keyword.java
    │   │               │   │   │   └── Subscription.java
    │   │               │   │   ├── repository
    │   │               │   │   │   ├── InterestRepository.java
    │   │               │   │   │   ├── InterestRepositoryCustom.java
    │   │               │   │   │   ├── InterestRepositoryImpl.java
    │   │               │   │   │   ├── KeywordRepository.java
    │   │               │   │   │   └── SubscriptionRepository.java
    │   │               │   │   └── service
    │   │               │   │       └── InterestService.java
    │   │               │   ├── notification
    │   │               │   │   ├── controller
    │   │               │   │   │   └── NotificationController.java
    │   │               │   │   ├── dto
    │   │               │   │   │   ├── NotificationDto.java
    │   │               │   │   │   └── NotificationListDto.java
    │   │               │   │   ├── entity
    │   │               │   │   │   ├── CommentNotification.java
    │   │               │   │   │   ├── InterestNotification.java
    │   │               │   │   │   └── Notification.java
    │   │               │   │   ├── event
    │   │               │   │   │   ├── BulkArticleRegisteredEvent.java
    │   │               │   │   │   └── CommentLikedEvent.java
    │   │               │   │   ├── listener
    │   │               │   │   │   └── NotificationListener.java
    │   │               │   │   ├── mapper
    │   │               │   │   │   └── NotificationMapper.java
    │   │               │   │   ├── repository
    │   │               │   │   │   ├── NotificationQueryRepository.java
    │   │               │   │   │   ├── NotificationRepository.java
    │   │               │   │   │   └── impl
    │   │               │   │   │       └── NotificationQueryRepositoryImpl.java
    │   │               │   │   ├── scheduler
    │   │               │   │   │   └── NotificationScheduler.java
    │   │               │   │   └── service
    │   │               │   │       └── NotificationService.java
    │   │               │   ├── user
    │   │               │   │   ├── controller
    │   │               │   │   │   └── UserController.java
    │   │               │   │   ├── dto
    │   │               │   │   │   ├── UserDto.java
    │   │               │   │   │   ├── UserLoginRequest.java
    │   │               │   │   │   ├── UserRegisterRequest.java
    │   │               │   │   │   └── UserUpdateRequest.java
    │   │               │   │   ├── entity
    │   │               │   │   │   └── User.java
    │   │               │   │   ├── mapper
    │   │               │   │   │   └── UserMapper.java
    │   │               │   │   ├── repository
    │   │               │   │   │   └── UserRepository.java
    │   │               │   │   ├── scheduler
    │   │               │   │   │   └── UserScheduler.java
    │   │               │   │   └── service
    │   │               │   │       └── UserService.java
    │   │               │   └── useractivity
    │   │               │       ├── controller
    │   │               │       │   └── UserActivityController.java
    │   │               │       ├── dto
    │   │               │       │   └── UserActivityDto.java
    │   │               │       ├── entity
    │   │               │       │   └── UserActivity.java
    │   │               │       ├── event
    │   │               │       │   ├── ArticleViewedEvent.java
    │   │               │       │   ├── CommentCreatedEvent.java
    │   │               │       │   ├── CommentLikedCancelEvent.java
    │   │               │       │   ├── CommentLikedEvent.java
    │   │               │       │   ├── CommentUpdatedEvent.java
    │   │               │       │   ├── InterestDeletedEvent.java
    │   │               │       │   ├── InterestSubscribedEvent.java
    │   │               │       │   ├── InterestUnSubscribedEvent.java
    │   │               │       │   └── UserRegisteredEvent.java
    │   │               │       ├── listener
    │   │               │       │   └── UserActivityEventListener.java
    │   │               │       ├── mapper
    │   │               │       │   └── UserActivityMapper.java
    │   │               │       ├── repository
    │   │               │       │   └── UserActivityRepository.java
    │   │               │       └── service
    │   │               │           └── UserActivityService.java
    │   │               ├── global
    │   │               │   ├── common
    │   │               │   │   ├── base
    │   │               │   │   │   ├── BaseEntity.java
    │   │               │   │   │   └── BaseUpdatableEntity.java
    │   │               │   │   └── constant
    │   │               │   │       └── ArticleSummaryConstants.java
    │   │               │   ├── config
    │   │               │   │   ├── AsyncConfig.java
    │   │               │   │   ├── AwsProperties.java
    │   │               │   │   ├── CacheConfig.java
    │   │               │   │   ├── JpaAuditingConfig.java
    │   │               │   │   ├── LlmClientTimeoutConfig.java
    │   │               │   │   ├── MDCLoggingInterceptor.java
    │   │               │   │   ├── QueryDslConfig.java
    │   │               │   │   ├── RestClientConfig.java
    │   │               │   │   ├── SwaggerConfig.java
    │   │               │   │   └── WebMvcConfig.java
    │   │               │   ├── exception
    │   │               │   │   ├── ErrorCode.java
    │   │               │   │   ├── ErrorResponse.java
    │   │               │   │   ├── GlobalExceptionHandler.java
    │   │               │   │   ├── Interest
    │   │               │   │   │   ├── AlreadySubscribedException.java
    │   │               │   │   │   ├── DuplicateInterestException.java
    │   │               │   │   │   ├── InterestException.java
    │   │               │   │   │   ├── InterestNotFoundException.java
    │   │               │   │   │   └── SubscriptionNotFoundException.java
    │   │               │   │   ├── MonewException.java
    │   │               │   │   ├── article
    │   │               │   │   │   ├── ArticleBackupBatchRunFailed.java
    │   │               │   │   │   ├── ArticleException.java
    │   │               │   │   │   ├── ArticleFileReadFailedException.java
    │   │               │   │   │   ├── ArticleFileSaveFailedException.java
    │   │               │   │   │   ├── ArticleNotFoundException.java
    │   │               │   │   │   ├── ArticleScrapeException.java
    │   │               │   │   │   └── InvalidArticleEntityException.java
    │   │               │   │   ├── aws
    │   │               │   │   │   ├── AwsException.java
    │   │               │   │   │   └── AwsServerConnectFailedException.java
    │   │               │   │   ├── comment
    │   │               │   │   │   ├── CommentException.java
    │   │               │   │   │   ├── CommentLikeAlreadyExistsException.java
    │   │               │   │   │   ├── CommentLikeNotFoundException.java
    │   │               │   │   │   ├── CommentNotFoundException.java
    │   │               │   │   │   └── CommentUpdateForbiddenException.java
    │   │               │   │   ├── common
    │   │               │   │   │   ├── CommonException.java
    │   │               │   │   │   ├── InvalidParameterException.java
    │   │               │   │   │   └── JsonParserFailedException.java
    │   │               │   │   ├── external
    │   │               │   │   │   ├── ExternalApiException.java
    │   │               │   │   │   ├── client
    │   │               │   │   │   │   ├── ExternalClientException.java
    │   │               │   │   │   │   ├── ExternalEmptyResponseException.java
    │   │               │   │   │   │   ├── ExternalNetworkException.java
    │   │               │   │   │   │   ├── ExternalRateLimitException.java
    │   │               │   │   │   │   └── ExternalServerException.java
    │   │               │   │   │   ├── crawl
    │   │               │   │   │   │   └── ExternalArticleCrawlException.java
    │   │               │   │   │   ├── llm
    │   │               │   │   │   │   ├── ExternalLlmException.java
    │   │               │   │   │   │   ├── ExternalLlmInvalidInputException.java
    │   │               │   │   │   │   └── ExternalLlmProviderException.java
    │   │               │   │   │   └── parser
    │   │               │   │   │       ├── EmptyXmlInputException.java
    │   │               │   │   │       └── ExternalInvalidXmlException.java
    │   │               │   │   ├── notification
    │   │               │   │   │   ├── NotificationAccessDeniedException.java
    │   │               │   │   │   ├── NotificationNotFoundException.java
    │   │               │   │   │   └── NotificationReceiverMismatchException.java
    │   │               │   │   └── user
    │   │               │   │       ├── DuplicateEmailException.java
    │   │               │   │       ├── PasswordMismatchException.java
    │   │               │   │       ├── UserAccessDeniedException.java
    │   │               │   │       ├── UserException.java
    │   │               │   │       └── UserNotFoundException.java
    │   │               │   └── logging
    │   │               │       ├── ClientIpResolver.java
    │   │               │       └── MaskingMessageConverter.java
    │   │               └── infra
    │   │                   ├── aws
    │   │                   │   └── S3Config.java
    │   │                   ├── external
    │   │                   │   ├── llm
    │   │                   │   │   ├── LlmSummarizer.java
    │   │                   │   │   ├── LlmSummaryService.java
    │   │                   │   │   ├── gemini
    │   │                   │   │   │   └── GeminiLlmSummarizer.java
    │   │                   │   │   └── openai
    │   │                   │   │       └── OpenAiLlmSummarizer.java
    │   │                   │   └── rss
    │   │                   │       ├── ArticleBodyCrawler.java
    │   │                   │       ├── CommonArticleCrawler.java
    │   │                   │       ├── HankyungCrawler.java
    │   │                   │       ├── NewsSourceUrl.java
    │   │                   │       ├── ResponseBodyDecoder.java
    │   │                   │       ├── XmlClient.java
    │   │                   │       └── XmlParser.java
    │   │                   ├── logging
    │   │                   │   ├── LogUploadController.java
    │   │                   │   ├── LogUploadProperties.java
    │   │                   │   ├── LogUploadResult.java
    │   │                   │   ├── LogUploadScheduler.java
    │   │                   │   └── LogUploadService.java
    │   │                   └── storage
    │   │                       └── s3
    │   │                           └── S3ArticleBackupFileStorage.java
    │   ├── main.iml
    │   └── resources
    │       ├── application-dev.yaml
    │       ├── application-local.yaml
    │       ├── application-prod.yaml
    │       ├── application.yaml
    │       ├── logback-spring.xml
    │       ├── schema-h2.sql
    │       ├── schema-postgre.sql
    │       ├── schema.sql
    │       └── static
    │           ├── assets
    │           │   ├── index-BBLciFoK.js
    │           │   ├── index-CHX_5t7G.css
    │           │   ├── landing_comments-BoMt6RvV.svg
    │           │   ├── landing_interests-CBQzCgwG.svg
    │           │   └── landing_notifications-BkwzqdfE.svg
    │           ├── favicon.ico
    │           ├── fonts
    │           │   └── pretendard
    │           │       ├── LICENSE.txt
    │           │       ├── Pretendard-Bold.woff2
    │           │       ├── Pretendard-Regular.woff2
    │           │       └── PretendardVariable.woff2
    │           └── index.html
    └── test
        ├── java
        │   └── com
        │       └── codeit
        │           └── monew
        │               ├── MonewApplicationTests.java
        │               ├── domain
        │               │   ├── article
        │               │   │   ├── controller
        │               │   │   │   └── ArticleControllerTest.java
        │               │   │   ├── entity
        │               │   │   │   └── ArticleEntityIntegrityTest.java
        │               │   │   ├── mapper
        │               │   │   │   └── ArticleBackupMapperTest.java
        │               │   │   ├── repository
        │               │   │   │   ├── ArticleRepositoryTest.java
        │               │   │   │   └── impl
        │               │   │   │       └── ArticleQueryRepositoryImplTest.java
        │               │   │   ├── scheduler
        │               │   │   │   ├── ArticleScrapeBatchConfigTest.java
        │               │   │   │   ├── ArticleScrapeBatchNotificationPublisherTest.java
        │               │   │   │   ├── ArticleScrapeResultExecutionContextManagerTest.java
        │               │   │   │   ├── NaverArticleBatchJobTest.java
        │               │   │   │   ├── NaverArticleScrapeTaskletTest.java
        │               │   │   │   ├── RssArticleBatchJobTest.java
        │               │   │   │   ├── RssArticleScrapeTaskletTest.java
        │               │   │   │   └── backup
        │               │   │   │       ├── ArticleBackupBatchConfigTest.java
        │               │   │   │       ├── ArticleBackupBatchRunnerTest.java
        │               │   │   │       ├── ArticleBackupJobTest.java
        │               │   │   │       └── ArticleBackupSchedulerTest.java
        │               │   │   └── service
        │               │   │       ├── ArticleBackupServiceTest.java
        │               │   │       ├── ArticleRestoreServiceTest.java
        │               │   │       ├── ArticleScrapeBatchRunnerTest.java
        │               │   │       ├── ArticleScrapePersistenceServiceTest.java
        │               │   │       ├── ArticleScrapeServiceTest.java
        │               │   │       └── ArticleServiceTest.java
        │               │   ├── comment
        │               │   │   ├── controller
        │               │   │   │   ├── CommentControllerTest.java
        │               │   │   │   └── CommentLikeControllerTest.java
        │               │   │   ├── repository
        │               │   │   │   ├── CommentLikeRepositoryTest.java
        │               │   │   │   ├── CommentRepositoryTest.java
        │               │   │   │   └── impl
        │               │   │   │       └── CommentQueryRepositoryImplTest.java
        │               │   │   └── service
        │               │   │       ├── CommentLikeServiceTest.java
        │               │   │       └── CommentServiceTest.java
        │               │   ├── interest
        │               │   │   ├── controller
        │               │   │   │   └── InterestControllerTest.java
        │               │   │   ├── repository
        │               │   │   │   └── InterestRepositoryTest.java
        │               │   │   └── service
        │               │   │       └── InterestServiceTest.java
        │               │   ├── notification
        │               │   │   ├── controller
        │               │   │   │   └── NotificationControllerTest.java
        │               │   │   ├── event
        │               │   │   │   └── BulkArticleRegisteredEventTest.java
        │               │   │   ├── listener
        │               │   │   │   └── NotificationListenerTest.java
        │               │   │   ├── repository
        │               │   │   │   ├── NotificationRepositoryTest.java
        │               │   │   │   └── impl
        │               │   │   │       └── NotificationQueryRepositoryImplTest.java
        │               │   │   ├── scheduler
        │               │   │   │   └── NotificationSchedulerTest.java
        │               │   │   └── service
        │               │   │       └── NotificationServiceTest.java
        │               │   ├── user
        │               │   │   ├── controller
        │               │   │   │   └── UserControllerTest.java
        │               │   │   ├── scheduler
        │               │   │   │   └── UserSchedulerTest.java
        │               │   │   └── service
        │               │   │       └── UserServiceTest.java
        │               │   └── useractivity
        │               │       ├── controller
        │               │       │   └── UserActivityControllerTest.java
        │               │       ├── listener
        │               │       │   └── UserActivityEventListenerTest.java
        │               │       └── service
        │               │           └── UserActivityServiceTest.java
        │               └── infra
        │                   ├── external
        │                   │   ├── llm
        │                   │   │   └── LlmSummaryServiceTest.java
        │                   │   └── rss
        │                   │       ├── ArticleBodyCrawlerTest.java
        │                   │       ├── XmlClientRestSliceTest.java
        │                   │       └── XmlParserTest.java
        │                   ├── logging
        │                   │   ├── LogUploadSchedulerTest.java
        │                   │   └── LogUploadServiceTest.java
        │                   └── storage
        │                       └── s3
        │                           └── S3ArticleBackupFileStorageTest.java
        ├── resources
        │   └── application-test.yaml
        └── test.iml
```
</details>

---
## 🔗 링크
- 서비스: [모뉴 바로가기](http://3.35.157.92/)
