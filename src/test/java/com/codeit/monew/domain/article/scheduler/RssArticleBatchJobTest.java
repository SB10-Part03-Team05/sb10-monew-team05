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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = RssArticleBatchJobTest.TestConfig.class)
@TestPropertySource(properties = {
    "retry.backoff.delay=10",
})
class RssArticleBatchJobTest {

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
    RssArticleBatchJob rssArticleBatchJob(ArticleScrapeService articleScrapeService,
        MeterRegistry meterRegistry) {
      return new RssArticleBatchJob(articleScrapeService, meterRegistry);
    }
  }

  @jakarta.annotation.Resource
  private RssArticleBatchJob rssArticleBatchJob;

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
    // Given: 한 번에 성공하여 기사 1개를 저장하는 시나리오
    when(articleScrapeService.scrapeAndSave(NewsSourceUrl.CHOSUN, null))
        .thenReturn(new ArticleScrapeResult(1, Map.of()));

    // When: RSS 배치 작업 실행
    ArticleScrapeResult result = rssArticleBatchJob.run(NewsSourceUrl.CHOSUN);

    // Then: 결과 확인 및 호출 횟수(1회) 검증
    assertEquals(1, result.totalSavedCount());
    verify(articleScrapeService, times(1)).scrapeAndSave(NewsSourceUrl.CHOSUN, null);
    assertEquals(1.0, meterRegistry.counter("scheduler.article.scrape.saved.total", "source",
        NewsSourceUrl.CHOSUN.name()).count());
    assertEquals(1.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", NewsSourceUrl.CHOSUN.name(),
            "status", "success", "error_type", "none").count());
  }

  @Test
  @DisplayName("실패(429): 재시도 대상이 아니므로 예외를 던진다")
  void rate_limit_no_retry_and_recover_empty() {
    // Given: Rate Limit(429) 예외가 발생하는 상황 설정
    when(articleScrapeService.scrapeAndSave(NewsSourceUrl.CHOSUN, null))
        .thenThrow(rateLimit());

    // When & Then: RSS배치 작업 실행 시 429 예외가 발생하면 던짐
    assertThrows(ExternalRateLimitException.class,
        () -> rssArticleBatchJob.run(NewsSourceUrl.CHOSUN));

    verify(articleScrapeService, times(1)).scrapeAndSave(NewsSourceUrl.CHOSUN, null);
    assertEquals(1.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", NewsSourceUrl.CHOSUN.name(),
            "status", "fail", "error_type", "RATE_LIMIT").count());
  }

  @Test
  @DisplayName("실패(4xx): 클라이언트 에러 발생 시 재시도 없이 예외를 던진다")
  void client_error_no_retry_and_recover_empty() {
    // Given: 클라이언트 에러(4xx) 발생 설정
    when(articleScrapeService.scrapeAndSave(NewsSourceUrl.CHOSUN, null))
        .thenThrow(clientError());

    // When & Then:  RSS배치 작업 실행 시 400 예외가 발생하면 던짐
    assertThrows(ExternalClientException.class, () -> rssArticleBatchJob.run(NewsSourceUrl.CHOSUN));

    verify(articleScrapeService, times(1)).scrapeAndSave(NewsSourceUrl.CHOSUN, null);
    assertEquals(1.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", NewsSourceUrl.CHOSUN.name(),
            "status", "fail", "error_type", "CLIENT_ERROR").count());
  }

  @Test
  @DisplayName("실패(INVALID_XML): 재시도 없이 ExternalInvalidXmlException을 던진다")
  void invalid_xml_no_retry_and_throws() {
    // given: RSS(조선일보) 수집 시 유효하지 않은 XML 응답 예외가 발생하도록 설정
    when(articleScrapeService.scrapeAndSave(NewsSourceUrl.CHOSUN, null))
        .thenThrow(invalidXml());

    // when & then: 배치 실행 시 즉시 예외가 발생해야 함을 검증
    assertThrows(
        ExternalInvalidXmlException.class,
        () -> rssArticleBatchJob.run(NewsSourceUrl.CHOSUN)
    );

    // then: 재시도 로직이 작동하지 않고 1회만 호출되었는지 확인
    verify(articleScrapeService, times(1)).scrapeAndSave(NewsSourceUrl.CHOSUN, null);

    // then: 메트릭에 에러 타입(INVALID_XML)과 출처(CHOSUN)가 정확히 기록되었는지 검증
    assertEquals(1.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", NewsSourceUrl.CHOSUN.name(),
            "status", "fail", "error_type", "INVALID_XML").count());
  }

  @Test
  @DisplayName("실패(EMPTY_XML): 재시도 없이 ExternalEmptyResponseException을 던진다")
  void empty_xml_no_retry_and_throws() {
    // given: RSS(조선일보) 수집 시 빈 응답 예외가 발생하도록 설정
    when(articleScrapeService.scrapeAndSave(NewsSourceUrl.CHOSUN, null))
        .thenThrow(emptyResponse());

    // when & then: 배치 실행 시 즉시 예외가 발생해야 함을 검증
    assertThrows(
        ExternalEmptyResponseException.class,
        () -> rssArticleBatchJob.run(NewsSourceUrl.CHOSUN)
    );

    // then: 재시도 로직이 작동하지 않고 1회만 호출되었는지 확인
    verify(articleScrapeService, times(1)).scrapeAndSave(NewsSourceUrl.CHOSUN, null);

    // then: 메트릭에 에러 타입(EMPTY_XML)과 출처(CHOSUN)가 정확히 기록되었는지 검증
    assertEquals(1.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", NewsSourceUrl.CHOSUN.name(),
            "status", "fail", "error_type", "EMPTY_XML").count());
  }

  @Test
  @DisplayName("실패(server/network): 일시적 장애 발생 시 재시도 후 성공하면 결과를 반환한다")
  void transient_retry_then_success() {
    // Given: 두 번의 일시적 장애 후 세 번째 시도에서 성공하는 시나리오
    when(articleScrapeService.scrapeAndSave(NewsSourceUrl.CHOSUN, null))
        .thenThrow(serverError())
        .thenThrow(networkError())
        .thenReturn(new ArticleScrapeResult(2, Map.of()));

    // When: RSS 배치 작업 실행
    ArticleScrapeResult result = rssArticleBatchJob.run(NewsSourceUrl.CHOSUN);

    // Then: 최종 성공 결과 확인 및 총 호출 횟수(3회) 검증
    assertEquals(2, result.totalSavedCount());
    verify(articleScrapeService, times(3)).scrapeAndSave(NewsSourceUrl.CHOSUN, null);
    assertEquals(2.0, meterRegistry.counter("scheduler.article.scrape.saved.total", "source",
        NewsSourceUrl.CHOSUN.name()).count());
    // 에러 2번 기록, 성공 1번 기록
    assertEquals(2.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", NewsSourceUrl.CHOSUN.name(),
            "status", "fail", "error_type", "SERVER_ERROR").count());
    assertEquals(1.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", NewsSourceUrl.CHOSUN.name(),
            "status", "success", "error_type", "none").count());
  }

  @Test
  @DisplayName("실패(server/network) 재시도 소진: 모든 재시도 실패 시 최종 예외를 던지며 종료한다")
  void transient_retry_exhausted_returns_empty() {
    // Given: 재시도 횟수를 모두 채울 때까지 계속 에러가 발생하는 상황
    when(articleScrapeService.scrapeAndSave(NewsSourceUrl.CHOSUN, null))
        .thenThrow(serverError());

    // When & Then: 모든 재시도(3회) 후 예외 전파
    assertThrows(ExternalServerException.class, () -> rssArticleBatchJob.run(NewsSourceUrl.CHOSUN));

    verify(articleScrapeService, times(3)).scrapeAndSave(NewsSourceUrl.CHOSUN, null);
    assertEquals(3.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", NewsSourceUrl.CHOSUN.name(),
            "status", "fail", "error_type", "SERVER_ERROR").count());
  }

  @Test
  @DisplayName("실패(알 수 없는 예외): 정책에 없는 예외는 즉시 예외를 전파한다")
  void unexpected_exception_propagates_immediately() {
    // Given: 일반 RuntimeException 발생 설정
    when(articleScrapeService.scrapeAndSave(NewsSourceUrl.CHOSUN, null))
        .thenThrow(new RuntimeException("boom"));

    // When & Then: 재시도나 복구 없이 즉시 예외가 던져지는지 확인
    assertThrows(RuntimeException.class, () -> rssArticleBatchJob.run(NewsSourceUrl.CHOSUN));
    verify(articleScrapeService, times(1)).scrapeAndSave(NewsSourceUrl.CHOSUN, null);
    assertEquals(1.0,
        meterRegistry.counter("scheduler.article.scrape.job", "source", NewsSourceUrl.CHOSUN.name(),
            "status", "fail", "error_type", "UNKNOWN").count());
  }

  // --- 테스트용 예외 생성 헬퍼 메서드 ---

  private ExternalRateLimitException rateLimit() {
    return new ExternalRateLimitException(
        NewsSourceUrl.CHOSUN, "https://x", HttpStatus.TOO_MANY_REQUESTS,
        new RuntimeException("429"));
  }

  private ExternalClientException clientError() {
    return new ExternalClientException(
        NewsSourceUrl.CHOSUN, "https://x", HttpStatus.BAD_REQUEST, new RuntimeException("400"));
  }

  private ExternalServerException serverError() {
    return new ExternalServerException(
        NewsSourceUrl.CHOSUN, "https://x", HttpStatus.SERVICE_UNAVAILABLE,
        new RuntimeException("500"));
  }

  private ExternalNetworkException networkError() {
    return new ExternalNetworkException(
        NewsSourceUrl.CHOSUN, "https://x", new RuntimeException("network"));
  }

  private ExternalInvalidXmlException invalidXml() {
    return new ExternalInvalidXmlException(
        NewsSourceUrl.CHOSUN, new RuntimeException("invalid xml"));
  }

  private ExternalEmptyResponseException emptyResponse() {
    return new ExternalEmptyResponseException(NewsSourceUrl.CHOSUN, "https://x");
  }
}
