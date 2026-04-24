package com.codeit.monew.domain.article.scheduler;

import com.codeit.monew.domain.interest.repository.KeywordRepository;
import com.codeit.monew.global.exception.external.ExternalClientException;
import com.codeit.monew.global.exception.external.ExternalNetworkException;
import com.codeit.monew.global.exception.external.ExternalRateLimitException;
import com.codeit.monew.global.exception.external.ExternalServerException;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverArticleBatchJob {

  private static final long NAVER_REQUEST_DELAY_MS = 50L;               // 요청 간격
  private static final int NAVER_RATE_LIMIT_MAX_RETRY = 5;              // 429, 재시도 최대 횟수
  private static final long NAVER_RATE_LIMIT_BASE_DELAY_MS = 100L;      // 429, 재시도 요청 간격 * 시도 횟수
  private static final int NAVER_SERVER_MAX_RETRY = 3;                  // 500, 재시도 최대 횟수
  private static final long NAVER_SERVER_RETRY_BASE_DELAY_MS = 10_000L; // 500, 재시도 요청 간격 * 시도 횟수

  private final NaverKeywordTxProcessor naverKeywordTxProcessor;
  private final KeywordRepository keywordRepository;

  public ArticleScrapeResult run() {
    Set<String> keywords = loadDistinctKeywordNames();
    int total = keywords.size();
    int index = 0;

    log.info("[NAVER_BATCH] start. keywordCount={}", total);

    ArticleScrapeResult totalResult = ArticleScrapeResult.empty();
    for (String keyword : keywords) {
      index++;
      sleep(NAVER_REQUEST_DELAY_MS);
      ArticleScrapeResult result = scrapeByKeyword(keyword, index, total);
      totalResult = totalResult.plus(result);
    }

    log.info("[NAVER_BATCH] finished. keywordCount={}, totalSaved={}", total, totalResult.totalSavedCount());
    return totalResult;
  }

  private ArticleScrapeResult scrapeByKeyword(String keyword, int index, int total) {
    int rateLimitAttempt = 0;
    int serverAttempt = 0;

    while (true) {
      try {
        ArticleScrapeResult result = naverKeywordTxProcessor.processOneKeyword(keyword);
        log.info("[NAVER_BATCH] keyword={}/{} ({}) saved={}", index, total, keyword, result.totalSavedCount());
        return result;
      } catch (ExternalRateLimitException e) {
        rateLimitAttempt++;
        if (rateLimitAttempt > NAVER_RATE_LIMIT_MAX_RETRY) {
          log.warn("[NAVER_BATCH] keyword={} rate-limit retry exhausted({})",
              keyword, NAVER_RATE_LIMIT_MAX_RETRY, e);
          return ArticleScrapeResult.empty();
        }
        long waitMs = NAVER_RATE_LIMIT_BASE_DELAY_MS * rateLimitAttempt;
        log.warn("[NAVER_BATCH] keyword={} 429 retry={}/{}, waitMs={}",
            keyword, rateLimitAttempt, NAVER_RATE_LIMIT_MAX_RETRY, waitMs, e);
        sleep(waitMs);
      } catch (ExternalServerException | ExternalNetworkException e) {
        serverAttempt++;
        if (serverAttempt > NAVER_SERVER_MAX_RETRY) {
          log.error("[NAVER_BATCH] keyword={} server retry exhausted({})",
              keyword, NAVER_SERVER_MAX_RETRY, e);
          return ArticleScrapeResult.empty();
        }
        long waitMs = NAVER_SERVER_RETRY_BASE_DELAY_MS * serverAttempt;
        log.warn("[NAVER_BATCH] keyword={} server retry={}/{}, waitMs={}",
            keyword, serverAttempt, NAVER_SERVER_MAX_RETRY, waitMs, e);
        sleep(waitMs);
      } catch (ExternalClientException e) {
        log.warn("[NAVER_BATCH] keyword={} client error, no retry", keyword, e);
        return ArticleScrapeResult.empty();
      } catch (Exception e) {
        // 예기치 못한 예외도 이 키워드만 실패 처리하고 다음 키워드로 진행
        log.error("[NAVER_BATCH] keyword={} unexpected error, skip this keyword", keyword, e);
        return ArticleScrapeResult.empty();
      }
    }
  }

  private Set<String> loadDistinctKeywordNames() {
    LinkedHashSet<String> result = new LinkedHashSet<>();
    keywordRepository.findAllWithInterest().forEach(keyword -> {
      String name = keyword.getName();
      if (StringUtils.hasText(name)) {
        result.add(name.trim());
      }
    });
    return result;
  }

  void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Naver batch sleep interrupted", e);
    }
  }
}