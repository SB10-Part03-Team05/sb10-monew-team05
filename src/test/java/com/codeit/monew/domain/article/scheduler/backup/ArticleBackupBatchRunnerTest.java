package com.codeit.monew.domain.article.scheduler.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.codeit.monew.global.exception.article.ArticleBackupBatchRunFailed;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

@ExtendWith(MockitoExtension.class)
class ArticleBackupBatchRunnerTest {

  @Mock
  private JobLauncher jobLauncher;

  @Mock
  private Job articleBackupBatchJob;

  @InjectMocks
  private ArticleBackupBatchRunner articleBackupBatchRunner;

  @Nested
  @DisplayName("뉴스 기사 백업 배치 실행")
  class run {

    @Test
    @DisplayName("백업 날짜를 JobParameter로 전달해 배치를 실행할 수 있다.")
    void success_run_article_backup_batch() throws Exception {
      // given
      LocalDate backupDate = LocalDate.of(2026, 4, 26);
      JobExecution jobExecution = new JobExecution(1L);
      jobExecution.setStatus(BatchStatus.COMPLETED);

      given(jobLauncher.run(any(Job.class), any(JobParameters.class))).willReturn(jobExecution);
      
      // when
      articleBackupBatchRunner.run(backupDate);

      // then
      ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
      verify(jobLauncher).run(eq(articleBackupBatchJob), captor.capture());

      JobParameters jobParameters = captor.getValue();
      assertEquals(backupDate, jobParameters.getLocalDate("backupDate"));
      assertNotNull(jobParameters.getLong("runId"));
    }

    @Test
    @DisplayName("Job 상태가 FAILED이면 500 상태코드와 ArticleBackupBatchRunFailed 예외가 발생한다.")
    void fail_run_when_job_status_failed() throws Exception {
      // given
      LocalDate backupDate = LocalDate.of(2026, 4, 26);
      JobExecution jobExecution = new JobExecution(1L);
      jobExecution.setStatus(BatchStatus.FAILED);

      given(jobLauncher.run(any(Job.class), any(JobParameters.class))).willReturn(jobExecution);

      // when & then
      assertThrows(ArticleBackupBatchRunFailed.class,
          () -> articleBackupBatchRunner.run(backupDate));
    }

    @Test
    @DisplayName("Job 상태가 STOPPED이면 500 상태코드와 ArticleBackupBatchRunFailed 예외가 발생한다.")
    void fail_run_when_job_status_stopped() throws Exception {
      // given
      LocalDate backupDate = LocalDate.of(2026, 4, 26);
      JobExecution jobExecution = new JobExecution(1L);
      jobExecution.setStatus(BatchStatus.STOPPED);

      given(jobLauncher.run(any(Job.class), any(JobParameters.class))).willReturn(jobExecution);

      // when & then
      assertThrows(ArticleBackupBatchRunFailed.class,
          () -> articleBackupBatchRunner.run(backupDate));
    }

    @Test
    @DisplayName("JobLauncher 실행 중 예외가 발생하면 500 상태코드와 ArticleBackupBatchRunFailed 예외가 발생한다.")
    void fail_run_when_job_launcher_throws_exception() throws Exception {
      // given
      LocalDate backupDate = LocalDate.of(2026, 4, 26);

      given(jobLauncher.run(any(Job.class), any(JobParameters.class)))
          .willThrow(new RuntimeException("batch failed"));

      // when & then
      assertThrows(ArticleBackupBatchRunFailed.class,
          () -> articleBackupBatchRunner.run(backupDate));
    }
  }
}
