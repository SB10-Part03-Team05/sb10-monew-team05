package com.codeit.monew.domain.article.scheduler;

import com.codeit.monew.domain.notification.event.BulkArticleRegisteredEvent;
import com.codeit.monew.domain.notification.event.BulkArticleRegisteredEvent.InterestArticleCount;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ArticleScrapeBatchConfig {

  public static final String JOB_NAME = "articleScrapeBatchJob";
  private static final String SCRAPE_RESULT_KEY = "scrapeResult";

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final RssArticleBatchJob rssArticleBatchJob;
  private final NaverArticleBatchJob naverArticleBatchJob;
  private final ApplicationEventPublisher eventPublisher;

  @Bean
  public Job articleScrapeBatchJob() {
    return new JobBuilder(JOB_NAME, jobRepository)
        .start(rssStep())
        .next(naverStep())
        .next(notificationStep())
        .build();
  }

  @Bean
  public Step rssStep() {
    return new StepBuilder("rssStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          ArticleScrapeResult stepResult = ArticleScrapeResult.empty();

          // 1. 네이버를 제외한 모든 RSS 소스 순회하며 수집
          for (NewsSourceUrl source : NewsSourceUrl.values()) {
            if (source != NewsSourceUrl.NAVER) {
              stepResult = stepResult.plus(rssArticleBatchJob.run(source));
            }
          }

          // 2. 전체 Job 결과에 합산하여 저장
          updateJobResult(chunkContext, stepResult);
          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }

  @Bean
  public Step naverStep() {
    return new StepBuilder("naverStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          // 1. 네이버 검색 API 이용 수집
          ArticleScrapeResult stepResult = naverArticleBatchJob.run();

          // 2. 전체 Job 결과에 합산하여 저장
          updateJobResult(chunkContext, stepResult);
          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }

  @Bean
  public Step notificationStep() {
    return new StepBuilder("notificationStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          ExecutionContext jobContext = chunkContext.getStepContext().getStepExecution()
              .getJobExecution().getExecutionContext();
          ArticleScrapeResult totalResult = (ArticleScrapeResult) jobContext.get(SCRAPE_RESULT_KEY);

          // 수집된 기사가 있다면 통합 알림 이벤트 발행
          if (totalResult != null && totalResult.totalSavedCount() > 0) {
            log.info("[ARTICLE_BATCH_JOB] 알림 발송 이벤트 발행 시작. 총 {}건", totalResult.totalSavedCount());

            List<InterestArticleCount> interestCounts = totalResult.results().entrySet().stream()
                .map(entry -> new InterestArticleCount(
                    entry.getKey(),
                    entry.getValue().name(),
                    entry.getValue().count()
                ))
                .toList();

            eventPublisher.publishEvent(new BulkArticleRegisteredEvent(interestCounts));
          }
          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }

  /**
   * 여러 Step에서 발생한 결과를 JobExecutionContext에 누적하여 저장합니다.
   */
  private void updateJobResult(org.springframework.batch.core.scope.context.ChunkContext chunkContext, ArticleScrapeResult stepResult) {
    ExecutionContext jobContext = chunkContext.getStepContext().getStepExecution()
        .getJobExecution().getExecutionContext();

    ArticleScrapeResult totalResult = (ArticleScrapeResult) jobContext.get(SCRAPE_RESULT_KEY);
    if (totalResult == null) {
      totalResult = stepResult;
    } else {
      totalResult = totalResult.plus(stepResult);
    }

    jobContext.put(SCRAPE_RESULT_KEY, totalResult);
  }
}
