package com.codeit.monew.domain.article.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.monew.domain.article.service.ArticleScrapeService;
import com.codeit.monew.global.exception.external.client.ExternalClientException;
import com.codeit.monew.global.exception.external.client.ExternalEmptyResponseException;
import com.codeit.monew.global.exception.external.client.ExternalNetworkException;
import com.codeit.monew.global.exception.external.client.ExternalRateLimitException;
import com.codeit.monew.global.exception.external.client.ExternalServerException;
import com.codeit.monew.global.exception.external.parser.ExternalInvalidXmlException;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = NaverArticleBatchJobTest.TestConfig.class)
class NaverArticleBatchJobTest {

  @Configuration
  @EnableRetry
  static class TestConfig {

    @Bean
    ArticleScrapeService articleScrapeService() {
      return mock(ArticleScrapeService.class);
    }

    @Bean
    MeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }

    @Bean
    NaverArticleBatchJob naverArticleBatchJob(ArticleScrapeService articleScrapeService,
        MeterRegistry meterRegistry) {
      return new NaverArticleBatchJob(articleScrapeService, meterRegistry);
    }
  }

  @jakarta.annotation.Resource
  private NaverArticleBatchJob naverArticleBatchJob;

  @jakarta.annotation.Resource
  private ArticleScrapeService articleScrapeService;

  @jakarta.annotation.Resource
  private MeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    reset(articleScrapeService);
    meterRegistry.clear();
  }

  @Test
  @DisplayName("성공(200): 재시도 없이 1회 처리한다")
  void success_once() {
    // Given: 한 번에 성공하는 시나리오 설정
    when(articleScrapeService.scrapeAndSave(NewsSourceUrl.NAVER, "삼성"))
        .thenReturn(new ArticleScrapeResult(1, Map.of()));

    // When: 비즈니스 로직 실행
    ArticleScrapeResult result = naverArticleBatchJob.run("삼성");

    // Then: 결과값 확인 및 호출 횟수(1회) 검증
    assertEquals(1, result.totalSavedCount());
    verify(articleScrapeService, times(1)).scrapeAndSave(NewsSourceUrl.NAVER, "삼성");
    assertEquals(1.0,
        meterRegistry.counter("scheduler.article.scrape.saved.total", "source", "NAVER").count());
    assertEquals(1.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", "NAVER", "status",
            "success", "error_type", "none").count());
  }

  @Test
  @DisplayName("실패(429): 재시도 후 성공하면 결과를 반환한다")
  void rate_limit_retry_then_success() {
    // Given: 첫 번째 시도에서 Rate Limit 발생 후 두 번째에 성공하는 시나리오
    when(articleScrapeService.scrapeAndSave(NewsSourceUrl.NAVER, "삼성"))
        .thenThrow(rateLimit())
        .thenReturn(new ArticleScrapeResult(2, Map.of()));

    // When: 비즈니스 로직 실행
    ArticleScrapeResult result = naverArticleBatchJob.run("삼성");

    // Then: 최종 성공 결과 확인 및 총 호출 횟수(2회) 검증
    assertEquals(2, result.totalSavedCount());
    verify(articleScrapeService, times(2)).scrapeAndSave(NewsSourceUrl.NAVER, "삼성");
    assertEquals(2.0,
        meterRegistry.counter("scheduler.article.scrape.saved.total", "source", "NAVER").count());
    // 첫 번째 시도 (실패 기록)
    assertEquals(1.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", "NAVER", "status", "fail",
            "error_type", "RATE_LIMIT").count());
    // 두 번째 시도 (성공 기록)
    assertEquals(1.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", "NAVER", "status",
            "success", "error_type", "none").count());
  }

  @Test
  @DisplayName("실패(429) 재시도 소진: 재시도 소진 후 최종적으로 ExternalRateLimitException을 던진다")
  void rate_limit_retry_exhausted_throws() {
    // Given: 모든 시도(3회)에서 계속 Rate Limit이 발생하는 상황
    when(articleScrapeService.scrapeAndSave(NewsSourceUrl.NAVER, "삼성"))
        .thenThrow(rateLimit());

    // When & Then: 재시도 횟수 초과 후 최종적으로 예외가 발생하는지 확인
    assertThrows(
        ExternalRateLimitException.class,
        () -> naverArticleBatchJob.run("삼성")
    );
    // 최대 시도 횟수(maxAttempts = 3)만큼 호출되었는지 검증
    verify(articleScrapeService, times(3)).scrapeAndSave(NewsSourceUrl.NAVER, "삼성");
    assertEquals(3.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", "NAVER", "status", "fail",
            "error_type", "RATE_LIMIT").count());
    // 성공 기록은 없어야 함: 0.0
    assertEquals(0.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", "NAVER", "status",
            "success", "error_type", "none").count());
  }

  @Test
  @DisplayName("실패(4xx): 4xx는 재시도 없이 ExternalClientException을 던진다")
  void client_error_no_retry_and_throws() {
    // Given: 재시도 대상이 아닌 Client Error(4xx) 발생 설정
    when(articleScrapeService.scrapeAndSave(NewsSourceUrl.NAVER, "삼성"))
        .thenThrow(clientError());

    // When & Then: 재시도 없이 즉시 예외가 전파되는지 확인
    assertThrows(
        ExternalClientException.class,
        () -> naverArticleBatchJob.run("삼성")
    );
    // 재시도 대상이 아니므로 호출 횟수는 1회여야 함
    verify(articleScrapeService, times(1)).scrapeAndSave(NewsSourceUrl.NAVER, "삼성");
    assertEquals(1.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", "NAVER", "status", "fail",
            "error_type", "CLIENT_ERROR").count());
  }

  @Test
  @DisplayName("실패(INVALID_XML): 재시도 없이 ExternalInvalidXmlException을 던진다")
  void invalid_xml_no_retry_and_throws() {
    // given: 네이버 뉴스 수집 시 유효하지 않은 XML 예외가 발생하도록 설정
    String keyword = "삼성";
    when(articleScrapeService.scrapeAndSave(NewsSourceUrl.NAVER, keyword))
        .thenThrow(invalidXml());

    // when & then: 배치 실행 시 예외가 발생해야 하며, 재시도 없이 1회만 호출되었는지 검증
    assertThrows(
        ExternalInvalidXmlException.class,
        () -> naverArticleBatchJob.run(keyword)
    );

    // 재시도 로직이 작동하지 않았음을 확인 (호출 횟수 1회)
    verify(articleScrapeService, times(1)).scrapeAndSave(NewsSourceUrl.NAVER, keyword);

    // 메트릭Registry에 INVALID_XML 에러 타입으로 실패 카운트가 기록되었는지 검증
    assertEquals(1.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", "NAVER", "status", "fail",
            "error_type", "INVALID_XML").count());
  }

  @Test
  @DisplayName("실패(EMPTY_XML): 재시도 없이 ExternalEmptyResponseException을 던진다")
  void empty_xml_no_retry_and_throws() {
    // given: 네이버 뉴스 수집 시 응답이 비어있는 예외가 발생하도록 설정
    String keyword = "삼성";
    when(articleScrapeService.scrapeAndSave(NewsSourceUrl.NAVER, keyword))
        .thenThrow(emptyResponse());

    // when & then: 배치 실행 시 예외가 발생해야 하며, 재시도 없이 1회만 호출되었는지 검증
    assertThrows(
        ExternalEmptyResponseException.class,
        () -> naverArticleBatchJob.run(keyword)
    );

    // 재시도 로직이 작동하지 않았음을 확인 (호출 횟수 1회)
    verify(articleScrapeService, times(1)).scrapeAndSave(NewsSourceUrl.NAVER, keyword);

    // 메트릭Registry에 EMPTY_XML 에러 타입으로 실패 카운트가 기록되었는지 검증
    assertEquals(1.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", "NAVER", "status", "fail",
            "error_type", "EMPTY_XML").count());
  }

  @Test
  @DisplayName("실패(server/network): 재시도 후 성공하면 결과를 반환한다")
  void transient_retry_then_success() {
    // Given: 서버 에러와 네트워크 에러가 번갈아 발생하다 마지막(3회째)에 성공하는 시나리오
    when(articleScrapeService.scrapeAndSave(NewsSourceUrl.NAVER, "삼성"))
        .thenThrow(serverError())
        .thenThrow(networkError())
        .thenReturn(new ArticleScrapeResult(3, Map.of()));

    // When: 비즈니스 로직 실행
    ArticleScrapeResult result = naverArticleBatchJob.run("삼성");

    // Then: 최종 결과 확인 및 총 호출 횟수(3회) 검증
    assertEquals(3, result.totalSavedCount());
    verify(articleScrapeService, times(3)).scrapeAndSave(NewsSourceUrl.NAVER, "삼성");
    assertEquals(3.0,
        meterRegistry.counter("scheduler.article.scrape.saved.total", "source", "NAVER").count());
    // 서버/네트워크 에러로 2번 실패 기록
    assertEquals(2.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", "NAVER", "status", "fail",
            "error_type", "SERVER_ERROR").count());
    // 1번 성공 기록
    assertEquals(1.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", "NAVER", "status",
            "success", "error_type", "none").count());
  }

  @Test
  @DisplayName("실패(server/network) 재시도 소진: 재시도 소진 후 최종적으로 ExternalServerException을 던진다")
  void transient_retry_exhausted_throws() {
    // Given: 일시적 장애가 계속되어 재시도 횟수를 모두 사용하는 상황
    when(articleScrapeService.scrapeAndSave(NewsSourceUrl.NAVER, "삼성"))
        .thenThrow(serverError());

    // When & Then: 최종적으로 예외가 발생하는지 확인
    assertThrows(
        ExternalServerException.class,
        () -> naverArticleBatchJob.run("삼성")
    );
    verify(articleScrapeService, times(3)).scrapeAndSave(NewsSourceUrl.NAVER, "삼성");
    assertEquals(3.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", "NAVER", "status", "fail",
            "error_type", "SERVER_ERROR").count());
  }

  @Test
  @DisplayName("실패(알 수 없는 예외): 재시도/복구 없이 즉시 전파한다")
  void unexpected_exception_propagates_immediately() {
    // Given: Retry 정책에 정의되지 않은 일반 런타임 예외 발생 설정
    when(articleScrapeService.scrapeAndSave(NewsSourceUrl.NAVER, "삼성"))
        .thenThrow(new RuntimeException("boom"));

    // When & Then: 아무런 재시도나 복구 로직 없이 즉시 예외 전파 확인
    assertThrows(RuntimeException.class, () -> naverArticleBatchJob.run("삼성"));
    verify(articleScrapeService, times(1)).scrapeAndSave(NewsSourceUrl.NAVER, "삼성");
    assertEquals(1.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", "NAVER", "status", "fail",
            "error_type", "UNKNOWN").count());
  }

  // --- 테스트용 예외 생성 헬퍼 메서드 ---

  private ExternalRateLimitException rateLimit() {
    return new ExternalRateLimitException(
        NewsSourceUrl.NAVER, "https://x", HttpStatus.TOO_MANY_REQUESTS,
        new RuntimeException("429"));
  }

  private ExternalClientException clientError() {
    return new ExternalClientException(
        NewsSourceUrl.NAVER, "https://x", HttpStatus.BAD_REQUEST, new RuntimeException("400"));
  }

  private ExternalServerException serverError() {
    return new ExternalServerException(
        NewsSourceUrl.NAVER, "https://x", HttpStatus.SERVICE_UNAVAILABLE,
        new RuntimeException("500"));
  }

  private ExternalNetworkException networkError() {
    return new ExternalNetworkException(
        NewsSourceUrl.NAVER, "https://x", new RuntimeException("network"));
  }

  private ExternalInvalidXmlException invalidXml() {
    return new ExternalInvalidXmlException(
        NewsSourceUrl.NAVER, new RuntimeException("invalid xml"));
  }

  private ExternalEmptyResponseException emptyResponse() {
    return new ExternalEmptyResponseException(NewsSourceUrl.NAVER, "https://x");
  }
}
