package com.codeit.monew.domain.article.scheduler;

import com.codeit.monew.global.exception.external.ExternalClientException;
import com.codeit.monew.global.exception.external.ExternalNetworkException;
import com.codeit.monew.global.exception.external.ExternalRateLimitException;
import com.codeit.monew.global.exception.external.ExternalServerException;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RssArticleBatchJob {

  private static final int RSS_SERVER_MAX_RETRY = 3;                  // 500, 최대 재시도 횟수
  private static final long RSS_SERVER_RETRY_BASE_DELAY_MS = 10_000L; // 500, 재시도 요청 간격 * attempt

  private final RssSourceTxProcessor rssSourceTxProcessor;

  public void run(NewsSourceUrl source) {
    int attempt = 0;

    while (true) {
      try {
        int saved = rssSourceTxProcessor.processOneSource(source);
        log.info("[RSS_BATCH] source={}, saved={}", source, saved);
        return;
      } catch (ExternalRateLimitException e) {
        log.warn("[RSS_BATCH] source={} rate-limited(429), skip until next batch", source, e);
        return;
      } catch (ExternalServerException | ExternalNetworkException e) {
        attempt++;
        if (attempt > RSS_SERVER_MAX_RETRY) {
          log.error("[RSS_BATCH] source={} failed after retries", source, e);
          return;
        }
        long waitMs = RSS_SERVER_RETRY_BASE_DELAY_MS * attempt;
        log.warn("[RSS_BATCH] source={} transient failure, retry={}/{}, waitMs={}",
            source, attempt, RSS_SERVER_MAX_RETRY, waitMs, e);
        sleep(waitMs);
      } catch (ExternalClientException e) {
        log.warn("[RSS_BATCH] source={} client error, no retry", source, e);
        return;
      } catch (Exception e) {
        log.error("[RSS_BATCH] source={} unexpected error, skip this source", source, e);
        return;
      }
    }
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("RSS batch sleep interrupted", e);
    }
  }
}