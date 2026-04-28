package com.codeit.monew.domain.article.scheduler;

import com.codeit.monew.global.exception.external.ExternalApiException;
import com.codeit.monew.global.exception.external.ExternalClientException;
import com.codeit.monew.global.exception.external.ExternalNetworkException;
import com.codeit.monew.global.exception.external.ExternalRateLimitException;
import com.codeit.monew.global.exception.external.ExternalServerException;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;
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
public class RssArticleBatchJob {

  private final RssSourceTxProcessor rssSourceTxProcessor;
  private final MeterRegistry meterRegistry;

  @Retryable(
      // 재시도 대상: 서버 에러 및 네트워크 장애 (일시적 오류)
      retryFor = {ExternalServerException.class, ExternalNetworkException.class},
      // 재시도 제외: 4xx 에러 및 429 에러
      noRetryFor = {ExternalClientException.class, ExternalRateLimitException.class},
      maxAttempts = 3,
      backoff = @Backoff(delayExpression = "${retry.backoff.delay:10000}", multiplier = 2)
  )
  public ArticleScrapeResult run(NewsSourceUrl source) {
    Timer.Sample sample = Timer.start(meterRegistry);
    String status = "success";
    String errorType = "none"; // 에러 유형 초기화
    String sourceName = source.name(); // 예: CHOSUN, JOONGANG

    try {
      ArticleScrapeResult result = rssSourceTxProcessor.processOneSource(source);
      if (result.totalSavedCount() > 0) {
        meterRegistry.counter("scheduler.article.scrape.saved.total", "source", sourceName)
            .increment(result.totalSavedCount());
      }

      log.info("[RSS_BATCH] source={}, saved={}", source, result.totalSavedCount());
      return result;
    } catch (ExternalRateLimitException e) {
      status = "fail";
      errorType = "RATE_LIMIT";
      log.warn("[RSS_BATCH] source={} rate-limited(429), skip source", source);
      throw e;
    } catch (ExternalClientException e) {
      status = "fail";
      errorType = "CLIENT_ERROR";
      log.warn("[RSS_BATCH] source={} client error(4xx), skip source", source);
      throw e;
    } catch (ExternalServerException | ExternalNetworkException e) {
      status = "fail";
      errorType = "SERVER_ERROR";
      log.warn("[RSS_BATCH] source={} transient failure, retrying...", source);
      throw e;
    } catch (Exception e) {
      status = "fail";
      errorType = "UNKNOWN";
      log.error("[RSS_BATCH] source={} unexpected error", source, e);
      throw e;
    } finally {
      meterRegistry.counter("scheduler.article.scrape.job",
          "source", sourceName,
          "status", status,
          "error_type", errorType).increment();
      sample.stop(meterRegistry.timer("scheduler.article.scrape.time",
          "source", sourceName,
          "status", status,
          "error_type", errorType));
    }
  }

  @Recover
  public ArticleScrapeResult recover(ExternalApiException e, NewsSourceUrl source) {
    log.error("[RSS_BATCH] source={} 처리 실패 (재시도 소진 또는 스킵). error={}",
        source, e.getMessage());
    return ArticleScrapeResult.empty();
  }
}