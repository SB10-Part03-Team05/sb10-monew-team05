package com.codeit.monew.domain.article.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.monew.global.exception.external.client.ExternalClientException;
import com.codeit.monew.global.exception.external.client.ExternalNetworkException;
import com.codeit.monew.global.exception.external.client.ExternalRateLimitException;
import com.codeit.monew.global.exception.external.client.ExternalServerException;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    RssSourceTxProcessor rssSourceTxProcessor() {
      return mock(RssSourceTxProcessor.class);
    }

    @Bean
    RssArticleBatchJob rssArticleBatchJob(RssSourceTxProcessor rssSourceTxProcessor) {
      return new RssArticleBatchJob(rssSourceTxProcessor);
    }
  }

  @jakarta.annotation.Resource
  private RssArticleBatchJob rssArticleBatchJob;

  @jakarta.annotation.Resource
  private RssSourceTxProcessor rssSourceTxProcessor;

  @BeforeEach
  void setUp() {
    reset(rssSourceTxProcessor);
  }

  @Test
  @DisplayName("성공(200): 재시도 없이 1회 처리한다")
  void success_once() {
    // Given: 한 번에 성공하여 기사 1개를 저장하는 시나리오
    when(rssSourceTxProcessor.processOneSource(NewsSourceUrl.CHOSUN))
        .thenReturn(new ArticleScrapeResult(1, Map.of()));

    // When: RSS 배치 작업 실행
    ArticleScrapeResult result = rssArticleBatchJob.run(NewsSourceUrl.CHOSUN);

    // Then: 결과 확인 및 호출 횟수(1회) 검증
    assertEquals(1, result.totalSavedCount());
    verify(rssSourceTxProcessor, times(1)).processOneSource(NewsSourceUrl.CHOSUN);
  }

  @Test
  @DisplayName("실패(429): 재시도 대상이 아니므로 빈 결과를 반환한다")
  void rate_limit_no_retry_and_recover_empty() {
    // Given: Rate Limit(429) 예외가 발생하는 상황 설정
    when(rssSourceTxProcessor.processOneSource(NewsSourceUrl.CHOSUN))
        .thenThrow(rateLimit());

    // When: RSS 배치 작업 실행
    ArticleScrapeResult result = rssArticleBatchJob.run(NewsSourceUrl.CHOSUN);

    // Then: 재시도 없이 recover가 작동하여 저장 수 0을 반환하는지 확인
    assertEquals(0, result.totalSavedCount());
    verify(rssSourceTxProcessor, times(1)).processOneSource(NewsSourceUrl.CHOSUN);
  }

  @Test
  @DisplayName("실패(4xx): 클라이언트 에러 발생 시 재시도 없이 빈 결과를 반환한다")
  void client_error_no_retry_and_recover_empty() {
    // Given: 클라이언트 에러(4xx) 발생 설정
    when(rssSourceTxProcessor.processOneSource(NewsSourceUrl.CHOSUN))
        .thenThrow(clientError());

    // When: RSS 배치 작업 실행
    ArticleScrapeResult result = rssArticleBatchJob.run(NewsSourceUrl.CHOSUN);

    // Then: 호출 횟수 1회 확인 및 빈 결과값 검증
    assertEquals(0, result.totalSavedCount());
    verify(rssSourceTxProcessor, times(1)).processOneSource(NewsSourceUrl.CHOSUN);
  }

  @Test
  @DisplayName("실패(server/network): 일시적 장애 발생 시 재시도 후 성공하면 결과를 반환한다")
  void transient_retry_then_success() {
    // Given: 두 번의 일시적 장애 후 세 번째 시도에서 성공하는 시나리오
    when(rssSourceTxProcessor.processOneSource(NewsSourceUrl.CHOSUN))
        .thenThrow(serverError())
        .thenThrow(networkError())
        .thenReturn(new ArticleScrapeResult(2, Map.of()));

    // When: RSS 배치 작업 실행
    ArticleScrapeResult result = rssArticleBatchJob.run(NewsSourceUrl.CHOSUN);

    // Then: 최종 성공 결과 확인 및 총 호출 횟수(3회) 검증
    assertEquals(2, result.totalSavedCount());
    verify(rssSourceTxProcessor, times(3)).processOneSource(NewsSourceUrl.CHOSUN);
  }

  @Test
  @DisplayName("실패(server/network) 재시도 소진: 모든 재시도 실패 시 빈 결과를 반환하며 종료한다")
  void transient_retry_exhausted_returns_empty() {
    // Given: 재시도 횟수를 모두 채울 때까지 계속 에러가 발생하는 상황
    when(rssSourceTxProcessor.processOneSource(NewsSourceUrl.CHOSUN))
        .thenThrow(serverError());

    // When: RSS 배치 작업 실행
    ArticleScrapeResult result = rssArticleBatchJob.run(NewsSourceUrl.CHOSUN);

    // Then: 예외가 상위로 던져지지 않고 recover를 통해 빈 결과(0)를 반환하는지 확인
    assertEquals(0, result.totalSavedCount());
    verify(rssSourceTxProcessor, times(3)).processOneSource(NewsSourceUrl.CHOSUN);
  }

  @Test
  @DisplayName("실패(알 수 없는 예외): 정책에 없는 예외는 즉시 예외를 전파한다")
  void unexpected_exception_propagates_immediately() {
    // Given: 일반 RuntimeException 발생 설정
    when(rssSourceTxProcessor.processOneSource(NewsSourceUrl.CHOSUN))
        .thenThrow(new RuntimeException("boom"));

    // When & Then: 재시도나 복구 없이 즉시 예외가 던져지는지 확인
    assertThrows(RuntimeException.class, () -> rssArticleBatchJob.run(NewsSourceUrl.CHOSUN));
    verify(rssSourceTxProcessor, times(1)).processOneSource(NewsSourceUrl.CHOSUN);
  }

  // --- 테스트용 예외 생성 헬퍼 메서드 ---

  private ExternalRateLimitException rateLimit() {
    return new ExternalRateLimitException(
        NewsSourceUrl.CHOSUN, "https://x", false, true, new RuntimeException("429"));
  }

  private ExternalClientException clientError() {
    return new ExternalClientException(
        NewsSourceUrl.CHOSUN, "https://x", 400, new RuntimeException("400"));
  }

  private ExternalServerException serverError() {
    return new ExternalServerException(
        NewsSourceUrl.CHOSUN, "https://x", 500, new RuntimeException("500"));
  }

  private ExternalNetworkException networkError() {
    return new ExternalNetworkException(
        NewsSourceUrl.CHOSUN, "https://x", new RuntimeException("network"));
  }
}