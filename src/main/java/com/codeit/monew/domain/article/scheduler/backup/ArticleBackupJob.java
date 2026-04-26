package com.codeit.monew.domain.article.scheduler.backup;

import com.codeit.monew.domain.article.service.ArticleBackupService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
// 백업 Step에서 실행되는 작업(Tasklet)
public class ArticleBackupJob implements Tasklet {

  private final ArticleBackupService articleBackupService;

  @Override
  public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws Exception {

    JobParameters jobParameters = chunkContext
        // 현재 Step과 관련된 컨텍스트를 가져옴
        .getStepContext()
        // 현재 실행 중인 StepExecution 정보를 가져옴
        .getStepExecution()
        // Job 실행 시 전달했던 JobParameters(Runner에서 `addLocalDate`, `addLong` 으로 추가한 값)
        .getJobParameters();

    // 백업 대상 날짜(ex: 2026-04-26)
    LocalDate backupDate = jobParameters.getLocalDate("backupDate");

    log.info("[ARTICLE_BACKUP_JOB] 뉴스 기사 백업 작업 시작: backupDate={}", backupDate);

    // 백업 비즈니스 로직 실행
    articleBackupService.backup(backupDate);

    log.info("[ARTICLE_BACKUP_JOB] 뉴스 기사 백업 작업 완료: backupDate={}", backupDate);

    // Tasklet 작업이 정상적으로 끝났다고 Spring Batch에 알림
    // FINISHED 반환 시 Step이 완료 처리됨
    return RepeatStatus.FINISHED;
  }
}
