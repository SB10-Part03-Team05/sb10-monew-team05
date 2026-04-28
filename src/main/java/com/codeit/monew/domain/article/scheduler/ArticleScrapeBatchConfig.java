package com.codeit.monew.domain.article.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class ArticleScrapeBatchConfig {

  public static final String JOB_NAME = "articleScrapeBatchJob";

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final RssArticleScrapeTasklet rssArticleScrapeTasklet;
  private final NaverArticleScrapeTasklet naverArticleScrapeTasklet;
  private final ArticleScrapeNotificationTasklet articleScrapeNotificationTasklet;

  @Bean
  public Job articleScrapeBatchJob() {
    return new JobBuilder(JOB_NAME, jobRepository)
        .start(rssStep())
        .on("FAILED").to(notificationStep())
        .from(rssStep())
        .on("COMPLETED").to(naverStep())
        .from(naverStep())
        .on("*").to(notificationStep())
        .end()
        .build();
  }

  @Bean
  public Step rssStep() {
    return new StepBuilder("rssStep", jobRepository)
        .tasklet(rssArticleScrapeTasklet, transactionManager)
        .build();
  }

  @Bean
  public Step naverStep() {
    return new StepBuilder("naverStep", jobRepository)
        .tasklet(naverArticleScrapeTasklet, transactionManager)
        .build();
  }

  @Bean
  public Step notificationStep() {
    return new StepBuilder("notificationStep", jobRepository)
        .tasklet(articleScrapeNotificationTasklet, transactionManager)
        .build();
  }
}
