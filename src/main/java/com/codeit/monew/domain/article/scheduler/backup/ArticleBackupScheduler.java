package com.codeit.monew.domain.article.scheduler.backup;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
// 정해진 시간에 백업 실행 요청
public class ArticleBackupScheduler {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final ArticleBackupBatchRunner articleBackupBatchRunner;

  @Scheduled(cron = "0 2 1 * * *", zone = "Asia/Seoul")
  public void backupYesterdayArticles() {
    LocalDate backupDate = LocalDate.now(KST).minusDays(1); // ex: 2026-04-26

    log.info("[ARTICLE_BACKUP_SCHEDULE] 뉴스 기사 백업 스케쥴 시작: backupDate={}", backupDate);

    articleBackupBatchRunner.run(backupDate);
  }
}
