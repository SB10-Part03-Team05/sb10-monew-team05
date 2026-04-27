package com.codeit.monew.domain.article.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.monew.global.exception.external.ExternalClientException;
import com.codeit.monew.global.exception.external.ExternalNetworkException;
import com.codeit.monew.global.exception.external.ExternalRateLimitException;
import com.codeit.monew.global.exception.external.ExternalServerException;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = NaverArticleBatchJobTest.TestConfig.class)
class NaverArticleBatchJobTest {

  @Configuration
  @EnableRetry
  static class TestConfig {

    @Bean
    NaverKeywordTxProcessor naverKeywordTxProcessor() {
      return mock(NaverKeywordTxProcessor.class);
    }

    @Bean
    NaverArticleBatchJob naverArticleBatchJob(NaverKeywordTxProcessor naverKeywordTxProcessor) {
      return new NaverArticleBatchJob(naverKeywordTxProcessor);
    }
  }

  @jakarta.annotation.Resource
  private NaverArticleBatchJob naverArticleBatchJob;

  @jakarta.annotation.Resource
  private NaverKeywordTxProcessor naverKeywordTxProcessor;

  @BeforeEach
  void setUp() {
    reset(naverKeywordTxProcessor);
  }

  @Test
  @DisplayName("성공(200): 재시도 없이 1회 처리한다")
  void success_once() {
    // Given: 한 번에 성공하는 시나리오 설정
    when(naverKeywordTxProcessor.processOneKeyword("삼성"))
        .thenReturn(new ArticleScrapeResult(1, Map.of()));

    // When: 비즈니스 로직 실행
    ArticleScrapeResult result = naverArticleBatchJob.run("삼성");

    // Then: 결과값 확인 및 호출 횟수(1회) 검증
    assertEquals(1, result.totalSavedCount());
    verify(naverKeywordTxProcessor, times(1)).processOneKeyword("삼성");
  }

  @Test
  @DisplayName("실패(429): 재시도 후 성공하면 결과를 반환한다")
  void rate_limit_retry_then_success() {
    // Given: 첫 번째 시도에서 Rate Limit 발생 후 두 번째에 성공하는 시나리오
    when(naverKeywordTxProcessor.processOneKeyword("삼성"))
        .thenThrow(rateLimit())
        .thenReturn(new ArticleScrapeResult(2, Map.of()));

    // When: 비즈니스 로직 실행
    ArticleScrapeResult result = naverArticleBatchJob.run("삼성");

    // Then: 최종 성공 결과 확인 및 총 호출 횟수(2회) 검증
    assertEquals(2, result.totalSavedCount());
    verify(naverKeywordTxProcessor, times(2)).processOneKeyword("삼성");
  }

  @Test
  @DisplayName("실패(429) 재시도 소진: recover에서 예외를 다시 던진다")
  void rate_limit_retry_exhausted_throws() {
    // Given: 모든 시도(3회)에서 계속 Rate Limit이 발생하는 상황
    when(naverKeywordTxProcessor.processOneKeyword("삼성"))
        .thenThrow(rateLimit());

    // When & Then: 재시도 횟수 초과 후 최종적으로 예외가 발생하는지 확인
    assertThrows(
        ExternalRateLimitException.class,
        () -> naverArticleBatchJob.run("삼성")
    );
    // 최대 시도 횟수(maxAttempts = 3)만큼 호출되었는지 검증
    verify(naverKeywordTxProcessor, times(3)).processOneKeyword("삼성");
  }

  @Test
  @DisplayName("실패(4xx): 재시도 없이 recover에서 예외를 다시 던진다")
  void client_error_no_retry_and_throws() {
    // Given: 재시도 대상이 아닌 Client Error(4xx) 발생 설정
    when(naverKeywordTxProcessor.processOneKeyword("삼성"))
        .thenThrow(clientError());

    // When & Then: 재시도 없이 즉시 예외가 전파되는지 확인
    assertThrows(
        ExternalClientException.class,
        () -> naverArticleBatchJob.run("삼성")
    );
    // 재시도 대상이 아니므로 호출 횟수는 1회여야 함
    verify(naverKeywordTxProcessor, times(1)).processOneKeyword("삼성");
  }

  @Test
  @DisplayName("실패(server/network): 재시도 후 성공하면 결과를 반환한다")
  void transient_retry_then_success() {
    // Given: 서버 에러와 네트워크 에러가 번갈아 발생하다 마지막(3회째)에 성공하는 시나리오
    when(naverKeywordTxProcessor.processOneKeyword("삼성"))
        .thenThrow(serverError())
        .thenThrow(networkError())
        .thenReturn(new ArticleScrapeResult(3, Map.of()));

    // When: 비즈니스 로직 실행
    ArticleScrapeResult result = naverArticleBatchJob.run("삼성");

    // Then: 최종 결과 확인 및 총 호출 횟수(3회) 검증
    assertEquals(3, result.totalSavedCount());
    verify(naverKeywordTxProcessor, times(3)).processOneKeyword("삼성");
  }

  @Test
  @DisplayName("실패(server/network) 재시도 소진: recover에서 예외를 다시 던진다")
  void transient_retry_exhausted_throws() {
    // Given: 일시적 장애가 계속되어 재시도 횟수를 모두 사용하는 상황
    when(naverKeywordTxProcessor.processOneKeyword("삼성"))
        .thenThrow(serverError());

    // When & Then: 최종적으로 예외가 발생하는지 확인
    assertThrows(
        ExternalServerException.class,
        () -> naverArticleBatchJob.run("삼성")
    );
    verify(naverKeywordTxProcessor, times(3)).processOneKeyword("삼성");
  }

  @Test
  @DisplayName("실패(알 수 없는 예외): 재시도/복구 없이 즉시 전파한다")
  void unexpected_exception_propagates_immediately() {
    // Given: Retry 정책에 정의되지 않은 일반 런타임 예외 발생 설정
    when(naverKeywordTxProcessor.processOneKeyword("삼성"))
        .thenThrow(new RuntimeException("boom"));

    // When & Then: 아무런 재시도나 복구 로직 없이 즉시 예외 전파 확인
    assertThrows(RuntimeException.class, () -> naverArticleBatchJob.run("삼성"));
    verify(naverKeywordTxProcessor, times(1)).processOneKeyword("삼성");
  }

  // --- 테스트용 예외 생성 헬퍼 메서드 ---

  private ExternalRateLimitException rateLimit() {
    return new ExternalRateLimitException(
        NewsSourceUrl.NAVER, "https://x", true, false, new RuntimeException("429"));
  }

  private ExternalClientException clientError() {
    return new ExternalClientException(
        NewsSourceUrl.NAVER, "https://x", 400, new RuntimeException("400"));
  }

  private ExternalServerException serverError() {
    return new ExternalServerException(
        NewsSourceUrl.NAVER, "https://x", 500, new RuntimeException("500"));
  }

  private ExternalNetworkException networkError() {
    return new ExternalNetworkException(
        NewsSourceUrl.NAVER, "https://x", new RuntimeException("network"));
  }
}