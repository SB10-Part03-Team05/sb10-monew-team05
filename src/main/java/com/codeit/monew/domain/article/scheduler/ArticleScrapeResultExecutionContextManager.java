package com.codeit.monew.domain.article.scheduler;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

@Component
public class ArticleScrapeResultExecutionContextManager {

  private static final String SCRAPE_RESULT_KEY = "scrapeResult";

  public void merge(ChunkContext chunkContext, ArticleScrapeResult stepResult) {
    ExecutionContext jobContext = getJobContext(chunkContext);
    ArticleScrapeResult totalResult = (ArticleScrapeResult) jobContext.get(SCRAPE_RESULT_KEY);

    if (totalResult == null) {
      jobContext.put(SCRAPE_RESULT_KEY, stepResult);
      return;
    }

    jobContext.put(SCRAPE_RESULT_KEY, totalResult.plus(stepResult));
  }

  public ArticleScrapeResult get(ChunkContext chunkContext) {
    ExecutionContext jobContext = getJobContext(chunkContext);
    return (ArticleScrapeResult) jobContext.get(SCRAPE_RESULT_KEY);
  }

  private ExecutionContext getJobContext(ChunkContext chunkContext) {
    return chunkContext.getStepContext().getStepExecution()
        .getJobExecution().getExecutionContext();
  }
}
