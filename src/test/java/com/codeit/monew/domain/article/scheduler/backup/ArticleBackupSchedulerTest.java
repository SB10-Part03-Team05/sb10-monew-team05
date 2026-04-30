package com.codeit.monew.domain.article.scheduler.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleBackupSchedulerTest {

  @Mock
  private ArticleBackupBatchRunner articleBackupBatchRunner;

  @InjectMocks
  private ArticleBackupScheduler articleBackupScheduler;

  @Nested
  @DisplayName("뉴스 기사 백업 스케줄러")
  class backupYesterdayArticles {

    @Test
    @DisplayName("KST 기준 전날 날짜로 백업 배치를 실행한다.")
    void success_backup_yesterday_articles() {
      // given(준비)
      LocalDate expectedBackupDate = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);

      // when(실행)
      articleBackupScheduler.backupYesterdayArticles();

      // then(검증)
      ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);

      verify(articleBackupBatchRunner).run(captor.capture());

      // `verify(..).run(captor.capture())` 이후 실행
      assertEquals(expectedBackupDate, captor.getValue());
    }
  }
}
