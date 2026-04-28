package com.codeit.monew.domain.article.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleScrapeNotificationTasklet implements Tasklet {

  private final ArticleScrapeResultExecutionContextManager contextManager;
  private final ArticleScrapeBatchNotificationPublisher notificationPublisher;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    ArticleScrapeResult totalResult = contextManager.get(chunkContext);
    notificationPublisher.publishIfNeeded(totalResult);
    return RepeatStatus.FINISHED;
  }
}
