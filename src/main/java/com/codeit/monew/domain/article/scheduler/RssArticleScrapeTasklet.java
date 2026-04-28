package com.codeit.monew.domain.article.scheduler;

import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RssArticleScrapeTasklet implements Tasklet {

  private final RssArticleBatchJob rssArticleBatchJob;
  private final ArticleScrapeResultExecutionContextManager contextManager;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    ArticleScrapeResult stepResult = ArticleScrapeResult.empty();

    try {
      for (NewsSourceUrl source : NewsSourceUrl.values()) {
        if (source == NewsSourceUrl.NAVER) {
          continue;
        }
        stepResult = stepResult.plus(rssArticleBatchJob.run(source));
      }
      return RepeatStatus.FINISHED;
    } finally {
      contextManager.merge(chunkContext, stepResult);
    }
  }
}
