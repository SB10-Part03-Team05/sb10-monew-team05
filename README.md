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

### 코드 품질
![CodeRabbit](https://img.shields.io/badge/CodeRabbit-FF6B35?style=flat-square)
![Codecov](https://img.shields.io/badge/Codecov-F01F7A?style=flat-square&logo=codecov&logoColor=white)
![k6](https://img.shields.io/badge/k6-7D64FF?style=flat-square&logo=k6&logoColor=white)

---
## 📚 팀원별 구현 기능 상세
### 박정현
### 박나경
### 박린
### 박성국
### 이규빈

---
## 디렉토리 구조 (기능 개발 마친 후 최종 수정 예정 !!!!)
```
.
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── codeit
│   │   │           └── monew
│   │   │               ├── domain
│   │   │               │   ├── article
│   │   │               │   │   ├── controller
│   │   │               │   │   │   ├── AdminArticleController.java
│   │   │               │   │   │   └── ArticleController.java
│   │   │               │   │   ├── dto
│   │   │               │   │   │   ├── request
│   │   │               │   │   │   │   └── ArticleSearchRequest.java
│   │   │               │   │   │   └── response
│   │   │               │   │   │       ├── ArticleDto.java
│   │   │               │   │   │       ├── ArticleScrapeBatchRunResponse.java
│   │   │               │   │   │       ├── ArticleViewDto.java
│   │   │               │   │   │       └── CursorPageResponseArticleDto.java
│   │   │               │   │   ├── entity
│   │   │               │   │   │   ├── type
│   │   │               │   │   │   │   ├── ArticleDirection.java
│   │   │               │   │   │   │   └── ArticleOrderBy.java
│   │   │               │   │   │   ├── Article.java
│   │   │               │   │   │   ├── ArticleInterest.java
│   │   │               │   │   │   ├── ArticleInterestId.java
│   │   │               │   │   │   └── ArticleViewHistory.java
│   │   │               │   │   ├── mapper
│   │   │               │   │   │   ├── ArticleMapper.java
│   │   │               │   │   │   └── ArticleViewMapper.java
│   │   │               │   │   ├── repository
│   │   │               │   │   │   ├── impl
│   │   │               │   │   │   │   └── ArticleQueryRepositoryImpl.java
│   │   │               │   │   │   ├── ArticleQueryRepository.java
│   │   │               │   │   │   ├── ArticleRepository.java
│   │   │               │   │   │   └── ArticleViewHistoryRepository.java
│   │   │               │   │   ├── scheduler
│   │   │               │   │   │   ├── ArticleScrapeBatchConfig.java
│   │   │               │   │   │   ├── ArticleScrapeBatchNotificationPublisher.java
│   │   │               │   │   │   ├── ArticleScrapeNotificationTasklet.java
│   │   │               │   │   │   ├── ArticleScrapeResult.java
│   │   │               │   │   │   ├── ArticleScrapeResultExecutionContextManager.java
│   │   │               │   │   │   ├── ArticleScrapeScheduler.java
│   │   │               │   │   │   ├── BatchCircuitBreaker.java
│   │   │               │   │   │   ├── NaverArticleBatchJob.java
│   │   │               │   │   │   ├── NaverArticleScrapeTasklet.java
│   │   │               │   │   │   ├── NaverKeywordTxProcessor.java
│   │   │               │   │   │   ├── RssArticleBatchJob.java
│   │   │               │   │   │   ├── RssArticleScrapeTasklet.java
│   │   │               │   │   │   └── RssSourceTxProcessor.java
│   │   │               │   │   ├── service
│   │   │               │   │   │   ├── ArticleScrapeBatchRunner.java
│   │   │               │   │   │   ├── ArticleScrapeService.java
│   │   │               │   │   │   └── ArticleService.java
│   │   │               │   │   └── ArticleSource.java
│   │   │               │   ├── comment
│   │   │               │   │   ├── controller
│   │   │               │   │   │   ├── CommentController.java
│   │   │               │   │   │   └── CommentLikeController.java
│   │   │               │   │   ├── dto
│   │   │               │   │   │   ├── CommentCursorRequest.java
│   │   │               │   │   │   ├── CommentDto.java
│   │   │               │   │   │   ├── CommentLikeDto.java
│   │   │               │   │   │   ├── CommentRegisterRequest.java
│   │   │               │   │   │   ├── CommentUpdateRequest.java
│   │   │               │   │   │   └── CursorPageResponseCommentDto.java
│   │   │               │   │   ├── entity
│   │   │               │   │   │   ├── Comment.java
│   │   │               │   │   │   └── CommentLike.java
│   │   │               │   │   ├── mapper
│   │   │               │   │   │   └── CommentMapper.java
│   │   │               │   │   ├── repository
│   │   │               │   │   │   ├── impl
│   │   │               │   │   │   │   └── CommentQueryRepositoryImpl.java
│   │   │               │   │   │   ├── CommentLikeRepository.java
│   │   │               │   │   │   ├── CommentQueryRepository.java
│   │   │               │   │   │   └── CommentRepository.java
│   │   │               │   │   └── service
│   │   │               │   │       ├── CommentLikeService.java
│   │   │               │   │       └── CommentService.java
│   │   │               │   ├── interest
│   │   │               │   │   ├── controller
│   │   │               │   │   │   └── InterestController.java
│   │   │               │   │   ├── dto
│   │   │               │   │   │   ├── request
│   │   │               │   │   │   │   ├── InterestRegisterRequest.java
│   │   │               │   │   │   │   └── InterestUpdateRequest.java
│   │   │               │   │   │   └── response
│   │   │               │   │   │       ├── CursorPageResponseInterestDto.java
│   │   │               │   │   │       ├── InterestDto.java
│   │   │               │   │   │       └── SubscriptionDto.java
│   │   │               │   │   ├── entity
│   │   │               │   │   │   ├── Interest.java
│   │   │               │   │   │   ├── Keyword.java
│   │   │               │   │   │   └── Subscription.java
│   │   │               │   │   ├── repository
│   │   │               │   │   │   ├── InterestRepository.java
│   │   │               │   │   │   ├── InterestRepositoryCustom.java
│   │   │               │   │   │   ├── InterestRepositoryImpl.java
│   │   │               │   │   │   ├── KeywordRepository.java
│   │   │               │   │   │   └── SubscriptionRepository.java
│   │   │               │   │   └── service
│   │   │               │   │       └── InterestService.java
│   │   │               │   ├── notification
│   │   │               │   │   ├── controller
│   │   │               │   │   │   └── NotificationController.java
│   │   │               │   │   ├── dto
│   │   │               │   │   │   ├── NotificationDto.java
│   │   │               │   │   │   └── NotificationListDto.java
│   │   │               │   │   ├── entity
│   │   │               │   │   │   ├── CommentNotification.java
│   │   │               │   │   │   ├── InterestNotification.java
│   │   │               │   │   │   └── Notification.java
│   │   │               │   │   ├── event
│   │   │               │   │   │   ├── BulkArticleRegisteredEvent.java
│   │   │               │   │   │   └── CommentLikedEvent.java
│   │   │               │   │   ├── listener
│   │   │               │   │   │   └── NotificationListener.java
│   │   │               │   │   ├── mapper
│   │   │               │   │   │   └── NotificationMapper.java
│   │   │               │   │   ├── repository
│   │   │               │   │   │   ├── impl
│   │   │               │   │   │   │   └── NotificationQueryRepositoryImpl.java
│   │   │               │   │   │   ├── NotificationQueryRepository.java
│   │   │               │   │   │   └── NotificationRepository.java
│   │   │               │   │   ├── scheduler
│   │   │               │   │   │   └── NotificationScheduler.java
│   │   │               │   │   └── service
│   │   │               │   │       └── NotificationService.java
│   │   │               │   ├── user
│   │   │               │   │   ├── controller
│   │   │               │   │   │   └── UserController.java
│   │   │               │   │   ├── dto
│   │   │               │   │   │   ├── UserDto.java
│   │   │               │   │   │   ├── UserLoginRequest.java
│   │   │               │   │   │   ├── UserRegisterRequest.java
│   │   │               │   │   │   └── UserUpdateRequest.java
│   │   │               │   │   ├── entity
│   │   │               │   │   │   └── User.java
│   │   │               │   │   ├── mapper
│   │   │               │   │   │   └── UserMapper.java
│   │   │               │   │   ├── repository
│   │   │               │   │   │   └── UserRepository.java
│   │   │               │   │   ├── scheduler
│   │   │               │   │   │   └── UserScheduler.java
│   │   │               │   │   └── service
│   │   │               │   │       └── UserService.java
│   │   │               │   └── useractivity
│   │   │               │       ├── controller
│   │   │               │       │   └── UserActivityController.java
│   │   │               │       ├── dto
│   │   │               │       │   └── UserActivityDto.java
│   │   │               │       ├── entity
│   │   │               │       │   └── UserActivity.java
│   │   │               │       ├── event
│   │   │               │       │   ├── ArticleViewedEvent.java
│   │   │               │       │   ├── CommentCreatedEvent.java
│   │   │               │       │   ├── CommentLikedCancelEvent.java
│   │   │               │       │   ├── CommentLikedEvent.java
│   │   │               │       │   ├── CommentUpdatedEvent.java
│   │   │               │       │   ├── InterestSubscribedEvent.java
│   │   │               │       │   ├── InterestUnSubscribedEvent.java
│   │   │               │       │   └── UserRegisteredEvent.java
│   │   │               │       ├── listener
│   │   │               │       │   └── UserActivityEventListener.java
│   │   │               │       ├── mapper
│   │   │               │       │   └── UserActivityMapper.java
│   │   │               │       ├── repository
│   │   │               │       │   └── UserActivityRepository.java
│   │   │               │       └── service
│   │   │               │           └── UserActivityService.java
│   │   │               ├── global
│   │   │               │   ├── common
│   │   │               │   │   └── base
│   │   │               │   │       ├── BaseEntity.java
│   │   │               │   │       └── BaseUpdatableEntity.java
│   │   │               │   ├── config
│   │   │               │   │   ├── AsyncConfig.java
│   │   │               │   │   ├── AwsProperties.java
│   │   │               │   │   ├── CacheConfig.java
│   │   │               │   │   ├── JpaAuditingConfig.java
│   │   │               │   │   ├── MDCLoggingInterceptor.java
│   │   │               │   │   ├── QueryDslConfig.java
│   │   │               │   │   ├── RestClientConfig.java
│   │   │               │   │   ├── SwaggerConfig.java
│   │   │               │   │   └── WebMvcConfig.java
│   │   │               │   ├── exception
│   │   │               │   │   ├── Interest
│   │   │               │   │   │   ├── AlreadySubscribedException.java
│   │   │               │   │   │   ├── DuplicateInterestException.java
│   │   │               │   │   │   ├── InterestException.java
│   │   │               │   │   │   ├── InterestNotFoundException.java
│   │   │               │   │   │   └── SubscriptionNotFoundException.java
│   │   │               │   │   ├── article
│   │   │               │   │   │   ├── ArticleException.java
│   │   │               │   │   │   ├── ArticleNotFoundException.java
│   │   │               │   │   │   ├── ArticleScrapeException.java
│   │   │               │   │   │   └── InvalidArticleEntityException.java
│   │   │               │   │   ├── comment
│   │   │               │   │   │   ├── CommentException.java
│   │   │               │   │   │   ├── CommentLikeAlreadyExistsException.java
│   │   │               │   │   │   ├── CommentLikeNotFoundException.java
│   │   │               │   │   │   ├── CommentNotFoundException.java
│   │   │               │   │   │   └── CommentUpdateForbiddenException.java
│   │   │               │   │   ├── common
│   │   │               │   │   │   ├── CommonException.java
│   │   │               │   │   │   └── InvalidParameterException.java
│   │   │               │   │   ├── external
│   │   │               │   │   │   ├── ExternalApiException.java
│   │   │               │   │   │   ├── ExternalClientException.java
│   │   │               │   │   │   ├── ExternalEmptyResponseException.java
│   │   │               │   │   │   ├── ExternalInvalidXmlException.java
│   │   │               │   │   │   ├── ExternalNetworkException.java
│   │   │               │   │   │   ├── ExternalRateLimitException.java
│   │   │               │   │   │   └── ExternalServerException.java
│   │   │               │   │   ├── notification
│   │   │               │   │   │   ├── NotificationAccessDeniedException.java
│   │   │               │   │   │   ├── NotificationNotFoundException.java
│   │   │               │   │   │   └── NotificationReceiverMismatchException.java
│   │   │               │   │   ├── user
│   │   │               │   │   │   ├── DuplicateEmailException.java
│   │   │               │   │   │   ├── PasswordMismatchException.java
│   │   │               │   │   │   ├── UserAccessDeniedException.java
│   │   │               │   │   │   ├── UserException.java
│   │   │               │   │   │   └── UserNotFoundException.java
│   │   │               │   │   ├── ErrorCode.java
│   │   │               │   │   ├── ErrorResponse.java
│   │   │               │   │   ├── GlobalExceptionHandler.java
│   │   │               │   │   └── MonewException.java
│   │   │               │   └── logging
│   │   │               │       ├── ClientIpResolver.java
│   │   │               │       └── MaskingMessageConverter.java
│   │   │               ├── infra
│   │   │               │   ├── aws
│   │   │               │   │   └── S3Config.java
│   │   │               │   ├── external
│   │   │               │   │   └── rss
│   │   │               │   │       ├── NewsSourceUrl.java
│   │   │               │   │       ├── XmlClient.java
│   │   │               │   │       └── XmlParser.java
│   │   │               │   └── logging
│   │   │               │       ├── LogUploadController.java
│   │   │               │       ├── LogUploadProperties.java
│   │   │               │       ├── LogUploadResult.java
│   │   │               │       ├── LogUploadScheduler.java
│   │   │               │       └── LogUploadService.java
│   │   │               └── MonewApplication.java
│   │   ├── resources
│   │   │   ├── static
│   │   │   │   ├── assets
│   │   │   │   │   ├── index-BBLciFoK.js
│   │   │   │   │   ├── index-CHX_5t7G.css
│   │   │   │   │   ├── landing_comments-BoMt6RvV.svg
│   │   │   │   │   ├── landing_interests-CBQzCgwG.svg
│   │   │   │   │   └── landing_notifications-BkwzqdfE.svg
│   │   │   │   ├── fonts
│   │   │   │   │   └── pretendard
│   │   │   │   │       ├── LICENSE.txt
│   │   │   │   │       ├── Pretendard-Bold.woff2
│   │   │   │   │       ├── Pretendard-Regular.woff2
│   │   │   │   │       └── PretendardVariable.woff2
│   │   │   │   ├── favicon.ico
│   │   │   │   └── index.html
│   │   │   ├── application-dev.yaml
│   │   │   ├── application-local.yaml
│   │   │   ├── application-prod.yaml
│   │   │   ├── application.yaml
│   │   │   ├── logback-spring.xml
│   │   │   ├── schema-h2.sql
│   │   │   ├── schema-postgre.sql
│   │   │   └── schema.sql
│   │   └── main.iml
│   └── test
│       ├── java
│       │   └── com
│       │       └── codeit
│       │           └── monew
│       │               ├── domain
│       │               │   ├── article
│       │               │   │   ├── controller
│       │               │   │   │   └── ArticleControllerTest.java
│       │               │   │   ├── entity
│       │               │   │   │   └── ArticleEntityIntegrityTest.java
│       │               │   │   ├── repository
│       │               │   │   │   ├── impl
│       │               │   │   │   │   └── ArticleQueryRepositoryImplTest.java
│       │               │   │   │   └── ArticleRepositoryTest.java
│       │               │   │   ├── scheduler
│       │               │   │   │   ├── ArticleScrapeBatchConfigTest.java
│       │               │   │   │   ├── ArticleScrapeBatchNotificationPublisherTest.java
│       │               │   │   │   ├── ArticleScrapeResultExecutionContextManagerTest.java
│       │               │   │   │   ├── NaverArticleBatchJobTest.java
│       │               │   │   │   ├── NaverArticleScrapeTaskletTest.java
│       │               │   │   │   ├── RssArticleBatchJobTest.java
│       │               │   │   │   └── RssArticleScrapeTaskletTest.java
│       │               │   │   └── service
│       │               │   │       ├── ArticleScrapeServiceTest.java
│       │               │   │       └── ArticleServiceTest.java
│       │               │   ├── comment
│       │               │   │   ├── controller
│       │               │   │   │   ├── CommentControllerTest.java
│       │               │   │   │   └── CommentLikeControllerTest.java
│       │               │   │   ├── repository
│       │               │   │   │   ├── impl
│       │               │   │   │   │   └── CommentQueryRepositoryImplTest.java
│       │               │   │   │   ├── CommentLikeRepositoryTest.java
│       │               │   │   │   └── CommentRepositoryTest.java
│       │               │   │   └── service
│       │               │   │       ├── CommentLikeServiceTest.java
│       │               │   │       └── CommentServiceTest.java
│       │               │   ├── interest
│       │               │   │   ├── controller
│       │               │   │   │   └── InterestControllerTest.java
│       │               │   │   ├── repository
│       │               │   │   │   └── InterestRepositoryTest.java
│       │               │   │   └── service
│       │               │   │       └── InterestServiceTest.java
│       │               │   ├── notification
│       │               │   │   ├── controller
│       │               │   │   │   └── NotificationControllerTest.java
│       │               │   │   ├── event
│       │               │   │   │   └── BulkArticleRegisteredEventTest.java
│       │               │   │   ├── listener
│       │               │   │   │   └── NotificationListenerTest.java
│       │               │   │   ├── repository
│       │               │   │   │   ├── impl
│       │               │   │   │   │   └── NotificationQueryRepositoryImplTest.java
│       │               │   │   │   └── NotificationRepositoryTest.java
│       │               │   │   ├── scheduler
│       │               │   │   │   └── NotificationSchedulerTest.java
│       │               │   │   └── service
│       │               │   │       └── NotificationServiceTest.java
│       │               │   ├── user
│       │               │   │   ├── controller
│       │               │   │   │   └── UserControllerTest.java
│       │               │   │   ├── scheduler
│       │               │   │   │   └── UserSchedulerTest.java
│       │               │   │   └── service
│       │               │   │       └── UserServiceTest.java
│       │               │   └── useractivity
│       │               │       ├── controller
│       │               │       │   └── UserActivityControllerTest.java
│       │               │       ├── listener
│       │               │       │   └── UserActivityEventListenerTest.java
│       │               │       └── service
│       │               │           └── UserActivityServiceTest.java
│       │               ├── infra
│       │               │   ├── external
│       │               │   │   └── rss
│       │               │   │       ├── XmlClientRestSliceTest.java
│       │               │   │       └── XmlParserTest.java
│       │               │   └── logging
│       │               │       └── LogUploadServiceTest.java
│       │               └── MonewApplicationTests.java
│       ├── resources
│       │   └── application-test.yaml
│       └── test.iml
├── Dockerfile
├── README.md
├── build.gradle
├── docker-compose.yaml
├── gradlew
├── gradlew.bat
├── prometheus.yml
└── settings.gradle
```

## 🔗 링크
- 서비스: [모뉴 바로가기](http://3.35.157.92/)
