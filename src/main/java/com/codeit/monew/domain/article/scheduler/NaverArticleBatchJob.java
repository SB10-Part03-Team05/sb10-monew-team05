package com.codeit.monew.domain.article.scheduler;

import com.codeit.monew.domain.interest.repository.KeywordRepository;
import com.codeit.monew.global.exception.external.ExternalApiException;
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

  private final NaverScrapeService naverScrapeService;
  private final KeywordRepository keywordRepository;
  private final BatchCircuitBreaker circuitBreaker;

  public ArticleScrapeResult run() {
    Set<String> keywords = loadDistinctKeywordNames();
    ArticleScrapeResult totalResult = ArticleScrapeResult.empty();

    log.info("[NAVER_BATCH] start. keywordCount={}", keywords.size());

    int currentOrder = 1;
    for (String keyword : keywords) {
      if (circuitBreaker.isBroken()) {
        log.error("[NAVER_BATCH] consecutive {} failures. stop early. remaining={}",
            circuitBreaker.getConsecutiveFailures(), keywords.size() - currentOrder + 1);
        break;
      }

      log.info("[NAVER_BATCH] >>> [{}/{}] Processing START: '{}'",
          currentOrder, keywords.size(), keyword);

      ArticleScrapeResult result = ArticleScrapeResult.empty();
      String status;

      try {
        result = naverScrapeService.scrapeWithRetry(keyword);
        circuitBreaker.recordSuccess();
        status = result.totalSavedCount() > 0 ? "SUCCESS" : "SUCCESS_EMPTY";
      } catch (ExternalApiException e) {
        circuitBreaker.recordFailure();
        status = "FAILED";
        log.warn(
            "[NAVER_BATCH] keyword='{}' failed. consecutiveFailures={}, errorType={}, message={}",
            keyword, circuitBreaker.getConsecutiveFailures(), e.getClass().getSimpleName(),
            e.getMessage());
      }

      log.info("[NAVER_BATCH] <<< [{}/{}] Processing END: '{}' - Status: {}, Saved: {}",
          currentOrder++, keywords.size(), keyword, status, result.totalSavedCount());

      totalResult = totalResult.plus(result);
      sleep(50L);
    }

    log.info("[NAVER_BATCH] finished. totalSaved={}", totalResult.totalSavedCount());
    return totalResult;
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
