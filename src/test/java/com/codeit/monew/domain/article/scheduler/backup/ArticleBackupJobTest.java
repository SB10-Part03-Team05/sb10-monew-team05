package com.codeit.monew.domain.article.scheduler.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.article.service.ArticleBackupService;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;

@ExtendWith(MockitoExtension.class)
class ArticleBackupJobTest {

  @Mock
  private ArticleBackupService articleBackupService;

  @InjectMocks
  private ArticleBackupJob articleBackupJob;

  private ChunkContext createChunkContext(LocalDate backupDate) {
    JobParameters jobParameters = new JobParametersBuilder()
        .addLocalDate("backupDate", backupDate)
        .toJobParameters();
    JobExecution jobExecution = new JobExecution(1L, jobParameters);
    StepExecution stepExecution = new StepExecution("articleBackupStep", jobExecution);

    return new ChunkContext(new StepContext(stepExecution));
  }

  @Nested
  @DisplayName("뉴스 기사 백업 Tasklet")
  class execute {

    @Test
    @DisplayName("JobParameter의 백업 날짜로 백업 서비스를 호출하고 FINISHED를 반환한다.")
    void success_execute_article_backup_job() throws Exception {
      // given
      LocalDate backupDate = LocalDate.of(2026, 4, 26);
      ChunkContext chunkContext = createChunkContext(backupDate);

      // when
      RepeatStatus result = articleBackupJob.execute(mock(StepContribution.class), chunkContext);

      // then
      assertEquals(RepeatStatus.FINISHED, result);
      verify(articleBackupService).backup(backupDate);
    }
  }
}
