package com.codeit.monew.domain.article.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NaverArticleScrapeTasklet implements Tasklet {

  private final NaverArticleBatchJob naverArticleBatchJob;
  private final ArticleScrapeResultExecutionContextManager contextManager;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    ArticleScrapeResult stepResult = naverArticleBatchJob.run();
    contextManager.merge(chunkContext, stepResult);
    return RepeatStatus.FINISHED;
  }
}
