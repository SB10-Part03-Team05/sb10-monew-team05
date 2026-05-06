package com.codeit.monew.domain.article.scheduler.backup;

import com.codeit.monew.global.exception.article.ArticleBackupBatchRunFailed;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
// JobLauncher로 배치 Job 실행
public class ArticleBackupBatchRunner {

  // Spring Batch Job을 실행하는 객체
  private final JobLauncher jobLauncher;
  // ArticleBackupBatchConfig 에서 `@Bean`으로 등록한 Job
  private final Job articleBackupBatchJob;

  public ArticleBackupBatchRunner(
      JobLauncher jobLauncher,
      @Qualifier("articleBackupBatchJob") Job articleBackupBatchJob
  ) {
    this.jobLauncher = jobLauncher;
    this.articleBackupBatchJob = articleBackupBatchJob;
  }

  public void run(LocalDate backupDate) {
    try {
      JobParameters jobParameters = new JobParametersBuilder()
          // Job 내부에서 사용할 백업 대상 날짜를 파라미터로 추가(Tasklet에서 backupDate 사용 가능)
          .addLocalDate("backupDate", backupDate)
          // Spring Batch는 이력을 저장 => 배치가 중간에 실패하면 같은 backupDate로 재실행/중복 실행이 불가능
          // => Job을 실행할 수 있게 매번 다른 값 추가
          .addLong("runId", System.currentTimeMillis())
          // Builder에서 만든 값을 JobParameters 객체로 변환
          .toJobParameters();

      log.info("[ARTICLE_BACKUP_BATCH_RUN] 뉴스 기사 백업 배치 실행: backupDate={}", backupDate);

      JobExecution execution = jobLauncher.run(articleBackupBatchJob, jobParameters);

      if (execution.getStatus() != BatchStatus.COMPLETED) {
        throw new IllegalStateException(
            "Article backup batch failed. jobExecutionId="
                + execution.getId() + ", status=" + execution.getStatus()
        );
      }

    } catch (Exception e) {
      throw new ArticleBackupBatchRunFailed(backupDate, e);
    }
  }
}
