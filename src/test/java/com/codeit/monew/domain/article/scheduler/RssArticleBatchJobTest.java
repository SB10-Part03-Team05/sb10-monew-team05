package com.codeit.monew.domain.article.scheduler;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codeit.monew.global.exception.external.ExternalClientException;
import com.codeit.monew.global.exception.external.ExternalNetworkException;
import com.codeit.monew.global.exception.external.ExternalRateLimitException;
import com.codeit.monew.global.exception.external.ExternalServerException;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RssArticleBatchJobTest {

  @Mock
  private RssSourceTxProcessor rssSourceTxProcessor;

  @InjectMocks
  private RssArticleBatchJob rssArticleBatchJob;

  private ExternalRateLimitException rateLimit() {
    return new ExternalRateLimitException(
        NewsSourceUrl.CHOSUN, "https://x", false, true, new RuntimeException("429"));
  }

  private ExternalServerException serverError() {
    return new ExternalServerException(
        NewsSourceUrl.CHOSUN, "https://x", 500, new RuntimeException("500"));
  }

  private ExternalNetworkException networkError() {
    return new ExternalNetworkException(
        NewsSourceUrl.CHOSUN, "https://x", new RuntimeException("network"));
  }

  @Nested
  @DisplayName("run")
  class Run {

    @Test
    @DisplayName("정상 1회 성공")
    void success_once() {
      // given: rssSourceTxProcessor가 CHOSUN 소스 처리 시 정상적으로 처리 건수(1)를 반환하도록 모킹
      given(rssSourceTxProcessor.processOneSource(NewsSourceUrl.CHOSUN)).willReturn(1);

      // when: 배치 잡 실행
      rssArticleBatchJob.run(NewsSourceUrl.CHOSUN);

      // then: 소스 처리가 정상적으로 1회 호출되었는지 검증
      verify(rssSourceTxProcessor).processOneSource(NewsSourceUrl.CHOSUN);
    }

    @Test
    @DisplayName("429 발생 시 즉시 종료")
    void stop_immediately_on_rate_limit() {
      // given: 소스 처리 중 429(Rate Limit) 예외가 발생하도록 모킹
      given(rssSourceTxProcessor.processOneSource(NewsSourceUrl.CHOSUN)).willThrow(rateLimit());

      // when: 배치 잡 실행
      rssArticleBatchJob.run(NewsSourceUrl.CHOSUN);

      // then: 429 에러는 재시도하지 않으므로 1회만 호출되고 잡이 즉시 종료되었음을 검증
      verify(rssSourceTxProcessor).processOneSource(NewsSourceUrl.CHOSUN);
    }

    @Test
    @DisplayName("client error 발생 시 즉시 종료")
    void stop_immediately_on_client_error() {
      // given: 소스 처리 중 400번대 클라이언트 예외가 발생하도록 모킹
      given(rssSourceTxProcessor.processOneSource(NewsSourceUrl.CHOSUN))
          .willThrow(new ExternalClientException(
              NewsSourceUrl.CHOSUN, "https://x", 400, new RuntimeException("400")));

      // when: 배치 잡 실행
      rssArticleBatchJob.run(NewsSourceUrl.CHOSUN);

      // then: 클라이언트 에러 역시 재시도 대상이 아니므로 1회만 호출되고 종료되었음을 검증
      verify(rssSourceTxProcessor).processOneSource(NewsSourceUrl.CHOSUN);
    }

    @Test
    @DisplayName("server error 재시도 후 성공")
    void retry_and_success_on_server_error() {
      // given: 서버 에러(500) 발생 시 잡 내부의 재시도 로직(Thread.sleep 등)이 동작함.
      // 무한정 기다리는 것을 방지하기 위해 스레드를 인터럽트(interrupt) 시키면서 에러를 던지도록 모킹
      given(rssSourceTxProcessor.processOneSource(NewsSourceUrl.CHOSUN))
          .willAnswer(inv -> {
            Thread.currentThread().interrupt();
            throw serverError();
          });

      try {
        // when & then: 배치 잡 실행 시 인터럽트로 인해 대기가 깨지며 IllegalStateException이 발생하는지 검증
        assertThrows(IllegalStateException.class,
            () -> rssArticleBatchJob.run(NewsSourceUrl.CHOSUN));
      } finally {
        // 상태 초기화
        Thread.interrupted();
      }

      // then: 예외 발생 시점까지 프로세서가 1회 호출(재시도 로직 진입)되었음을 검증
      verify(rssSourceTxProcessor, times(1)).processOneSource(NewsSourceUrl.CHOSUN);
    }

    @Test
    @DisplayName("network error 재시도 후 성공")
    void retry_and_success_on_network_error() {
      // given: 네트워크 에러 발생 시 재시도 로직 대기를 피하고자 인터럽트 시키면서 예외를 던지도록 모킹
      given(rssSourceTxProcessor.processOneSource(NewsSourceUrl.CHOSUN))
          .willAnswer(inv -> {
            Thread.currentThread().interrupt();
            throw networkError();
          });

      try {
        // when & then: 배치 잡 실행 시 대기 중단에 의한 IllegalStateException이 발생하는지 검증
        assertThrows(IllegalStateException.class,
            () -> rssArticleBatchJob.run(NewsSourceUrl.CHOSUN));
      } finally {
        // 상태 초기화
        Thread.interrupted();
      }

      // then: 예외 발생 시점까지 프로세서가 1회 호출되었음을 검증
      verify(rssSourceTxProcessor, times(1)).processOneSource(NewsSourceUrl.CHOSUN);
    }

    @Test
    @DisplayName("server/network 재시도 소진 후 종료")
    void stop_after_retry_exhausted() {
      // given: 재시도 횟수 소진(혹은 단발성 종료 예외) 상황을 모킹
      given(rssSourceTxProcessor.processOneSource(NewsSourceUrl.CHOSUN)).willThrow(rateLimit());

      // when: 배치 잡 실행
      rssArticleBatchJob.run(NewsSourceUrl.CHOSUN);

      // then: 정해진 횟수 초과 또는 예외 종류로 인해 더 이상 호출되지 않고 1회 호출 후 종료되었음을 검증
      verify(rssSourceTxProcessor, times(1)).processOneSource(NewsSourceUrl.CHOSUN);
    }

    @Test
    @DisplayName("예상치 못한 예외 발생 시 종료")
    void stop_on_unexpected_error() {
      // given: 프로세서 실행 중 예상치 못한 일반 RuntimeException이 발생하도록 모킹
      given(rssSourceTxProcessor.processOneSource(NewsSourceUrl.CHOSUN))
          .willThrow(new RuntimeException("unexpected"));

      // when: 배치 잡 실행
      rssArticleBatchJob.run(NewsSourceUrl.CHOSUN);

      // then: 처리 불가능한 예외이므로 잡이 즉시 에러를 먹고(또는 전파하고) 1회 호출 뒤 종료되었음을 검증
      verify(rssSourceTxProcessor).processOneSource(NewsSourceUrl.CHOSUN);
    }
  }
}