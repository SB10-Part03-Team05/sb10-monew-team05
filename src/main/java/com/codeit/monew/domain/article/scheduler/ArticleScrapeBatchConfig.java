package com.codeit.monew.domain.article.scheduler;

import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class ArticleScrapeBatchConfig {

  public static final String JOB_NAME = "articleScrapeBatchJob";

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final RssArticleBatchJob rssArticleBatchJob;
  private final NaverArticleBatchJob naverArticleBatchJob;

  @Bean
  public Job articleScrapeBatchJob() {
    return new JobBuilder(JOB_NAME, jobRepository)
        .start(rssStep())
        .next(naverStep())
        .build();
  }

  @Bean
  public Step rssStep() {
    return new StepBuilder("rssStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          Arrays.stream(NewsSourceUrl.values())
              .filter(source -> source
                  != NewsSourceUrl.NAVER) // 네이버를 제외한 NewsSourceUrl에 있는 모든 소스를 기준으로 배치 실행
              .forEach(rssArticleBatchJob::run);
          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }

  @Bean
  public Step naverStep() {
    return new StepBuilder("naverStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          naverArticleBatchJob.run();
          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }

}
