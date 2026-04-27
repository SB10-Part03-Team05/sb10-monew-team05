package com.codeit.monew.domain.article.scheduler;

import com.codeit.monew.global.exception.external.ExternalApiException;
import com.codeit.monew.global.exception.external.ExternalClientException;
import com.codeit.monew.global.exception.external.ExternalNetworkException;
import com.codeit.monew.global.exception.external.ExternalRateLimitException;
import com.codeit.monew.global.exception.external.ExternalServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverScrapeService {

  private final NaverKeywordTxProcessor naverKeywordTxProcessor;

  @Retryable(
      retryFor = {ExternalRateLimitException.class, ExternalServerException.class,
          ExternalNetworkException.class},
      noRetryFor = {ExternalClientException.class},
      maxAttempts = 4,
      backoff = @Backoff(delay = 500, multiplier = 2) // 0.5초 시작, 최대 4초 대기
  )
  public ArticleScrapeResult scrapeWithRetry(String keyword) {
    try {
      return naverKeywordTxProcessor.processOneKeyword(keyword);
    } catch (ExternalRateLimitException e) {
      log.warn("[NAVER_RETRY] keyword='{}' rate-limited(429), retrying...", keyword);
      throw e;
    } catch (ExternalClientException e) {
      log.warn("[NAVER_RETRY] keyword='{}' client error(4xx), no retry", keyword);
      throw e;
    } catch (ExternalServerException | ExternalNetworkException e) {
      log.warn("[NAVER_RETRY] keyword='{}' transient failure, retrying...", keyword);
      throw e;
    } catch (Exception e) {
      log.error("[NAVER_RETRY] keyword='{}' unexpected error", keyword, e);
      throw e;
    }
  }

  @Recover
  public ArticleScrapeResult recover(ExternalApiException e, String keyword) {
    log.error("[NAVER_RETRY] keyword='{}' 처리 실패 (재시도 소진 또는 스킵). error={}",
        keyword, e.getMessage());
    throw e;
  }
}
