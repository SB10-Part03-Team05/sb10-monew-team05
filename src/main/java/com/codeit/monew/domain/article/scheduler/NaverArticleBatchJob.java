package com.codeit.monew.domain.article.scheduler;

import com.codeit.monew.global.exception.external.ExternalApiException;
import com.codeit.monew.global.exception.external.ExternalClientException;
import com.codeit.monew.global.exception.external.ExternalNetworkException;
import com.codeit.monew.global.exception.external.ExternalRateLimitException;
import com.codeit.monew.global.exception.external.ExternalServerException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverArticleBatchJob {

  private final NaverKeywordTxProcessor naverKeywordTxProcessor;
  private final MeterRegistry meterRegistry;

  @Retryable(
      // 재시도 대상: 429에러, 서버 에러 및 네트워크 장애 (일시적 오류)
      retryFor = {ExternalRateLimitException.class, ExternalServerException.class,
          ExternalNetworkException.class},
      // 재시도 제외: 4xx 에러
      noRetryFor = {ExternalClientException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 100, multiplier = 10)
  )
  public ArticleScrapeResult run(String keyword) {
    Timer.Sample sample = Timer.start(meterRegistry);
    String status = "success";
    String errorType = "none"; // 에러 유형 초기화
    try {
      ArticleScrapeResult result = naverKeywordTxProcessor.processOneKeyword(keyword);
      if (result.totalSavedCount() > 0) {
        meterRegistry.counter("scheduler.article.scrape.saved.total", "source", "NAVER")
            .increment(result.totalSavedCount());
      }
      log.info("[NAVER_BATCH] keyword='{}', saved={}", keyword, result.totalSavedCount());
      return result;
    } catch (ExternalRateLimitException e) {
      status = "fail";
      errorType = "RATE_LIMIT";
      log.warn("[NAVER_BATCH] keyword='{}' rate-limited(429), retrying...", keyword);
      throw e;
    } catch (ExternalClientException e) {
      status = "fail";
      errorType = "CLIENT_ERROR";
      log.warn("[NAVER_BATCH] keyword='{}' client error(4xx), no retry", keyword);
      throw e;
    } catch (ExternalServerException | ExternalNetworkException e) {
      status = "fail";
      errorType = "SERVER_ERROR";
      log.warn("[NAVER_BATCH] keyword='{}' transient failure, retrying...", keyword);
      throw e;
    } catch (Exception e) {
      status = "fail";
      errorType = "UNKNOWN";
      log.error("[NAVER_BATCH] keyword='{}' unexpected error", keyword, e);
      throw e;
    } finally {
      meterRegistry.counter("scheduler.article.scrape.job",
          "source", "NAVER",
          "status", status,
          "error_type", errorType).increment();
      sample.stop(meterRegistry.timer("scheduler.article.scrape.time",
          "source", "NAVER",
          "status", status,
          "error_type", errorType));
    }
  }

  @Recover
  public ArticleScrapeResult recover(ExternalApiException e, String keyword) {
    log.error("[NAVER_BATCH] keyword='{}' 처리 실패 (재시도 소진 또는 스킵). error={}",
        keyword, e.getMessage());
    throw e;
  }
}
