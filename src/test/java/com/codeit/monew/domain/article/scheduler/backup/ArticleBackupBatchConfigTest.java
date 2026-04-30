package com.codeit.monew.domain.article.scheduler.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class ArticleBackupBatchConfigTest {

  @Mock
  private JobRepository jobRepository;

  @Mock
  private PlatformTransactionManager transactionManager;

  @Mock
  private ArticleBackupJob articleBackupJob;

  @Nested
  @DisplayName("뉴스 기사 백업 배치 설정")
  class BackupBatchConfig {

    @Test
    @DisplayName("뉴스 기사 백업 Job을 생성할 수 있다.")
    void success_create_article_backup_batch_job() {
      // given
      ArticleBackupBatchConfig config = new ArticleBackupBatchConfig(jobRepository,
          transactionManager, articleBackupJob);

      // when
      Job job = config.articleBackupBatchJob();

      // then
      assertNotNull(job);
      assertEquals("articleBackupJob", job.getName());
    }

    @Test
    @DisplayName("뉴스 기사 백업 Step을 생성할 수 있다.")
    void success_create_article_backup_step() {
      // given
      ArticleBackupBatchConfig config = new ArticleBackupBatchConfig(jobRepository,
          transactionManager, articleBackupJob);

      // when
      Step step = config.articleBackupStep();

      // then
      assertNotNull(step);
      assertEquals("articleBackupStep", step.getName());
    }
  }
}
