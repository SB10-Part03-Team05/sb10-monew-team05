package com.codeit.monew.domain.article.scheduler;

import com.codeit.monew.domain.interest.repository.KeywordRepository;
import com.codeit.monew.global.exception.external.ExternalApiException;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverArticleScrapeTasklet implements Tasklet {

  private final NaverArticleBatchJob naverArticleBatchJob;
  private final KeywordRepository keywordRepository;
  private final BatchCircuitBreaker circuitBreaker;
  private final ArticleScrapeResultExecutionContextManager contextManager;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    ArticleScrapeResult totalResult = ArticleScrapeResult.empty();

    try {
      circuitBreaker.reset();
      Set<String> keywords = loadDistinctKeywordNames();
      log.info("[NAVER_BATCH] start. keywordCount={}", keywords.size());

      int currentOrder = 1;
      for (String keyword : keywords) {
        if (circuitBreaker.isBroken()) {
          log.error("[NAVER_BATCH] 연속 실패 횟수 {}회 도달. 조기 종료 수행. 남은 키워드 수: {}",
              circuitBreaker.getConsecutiveFailures(), keywords.size() - currentOrder + 1);
          break;
        }

        ArticleScrapeResult result = scrapeSingleKeyword(keyword, currentOrder++, keywords.size());
        totalResult = totalResult.plus(result);
        sleep(50L);
      }

      log.info("[NAVER_BATCH] finished. totalSaved={}", totalResult.totalSavedCount());
      return RepeatStatus.FINISHED;
    } finally {
      contextManager.merge(chunkContext, totalResult);
    }
  }

  private ArticleScrapeResult scrapeSingleKeyword(String keyword, int order, int total) {
    log.info("[NAVER_BATCH] >>> [{}/{}] Processing START: '{}'", order, total, keyword);

    try {
      ArticleScrapeResult result = naverArticleBatchJob.run(keyword);
      circuitBreaker.recordSuccess();

      log.info("[NAVER_BATCH] <<< [{}/{}] Processing END: '{}' - Status: SUCCESS, Saved: {}",
          order, total, keyword, result.totalSavedCount());

      return result;

    } catch (ExternalApiException e) {
      circuitBreaker.recordFailure();

      log.warn("[NAVER_BATCH] keyword='{}' failed. consecutiveFailures={}, message={}",
          keyword, circuitBreaker.getConsecutiveFailures(), e.getMessage(), e);

      return ArticleScrapeResult.empty();
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
      log.warn("naver batch sleep interrupted");
    }
  }
}
