package com.codeit.monew.domain.article.scheduler.backup;

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
// Spring Batch Job/Step Bean 설정
public class ArticleBackupBatchConfig {

  // Spring Batch 실행 정보를 저장하고 관리하는 저장소
  private final JobRepository jobRepository;
  // Step 실행 중 트랜잭션을 관리하는 객체
  private final PlatformTransactionManager transactionManager;
  // 실제 백업 작업을 수행하는 Tasklet
  private final ArticleBackupJob articleBackupJob;

  @Bean
  // Job 객체를 Spring Bean으로 등록
  public Job articleBackupBatchJob() {
    // "articleBackupJob"이라는 Spring Batch Job을 생성
    return new JobBuilder("articleBackupJob", jobRepository)
        .start(articleBackupStep()) // Job이 실행할 Step
        .build();
  }

  @Bean
  // Step 객체를 Spring Bean으로 등록
  public Step articleBackupStep() {
    return new StepBuilder("articleBackupStep", jobRepository)
        // Step에서 실행할 작업을 Tasklet 방식으로 지정 후 articleBackupJob이 작업 수행
        .tasklet(articleBackupJob, transactionManager)
        .build();
  }
}
