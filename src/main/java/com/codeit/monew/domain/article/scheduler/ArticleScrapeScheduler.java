package com.codeit.monew.domain.article.scheduler;

import com.codeit.monew.domain.article.service.ArticleScrapeBatchRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleScrapeScheduler {

  private final ArticleScrapeBatchRunner articleScrapeBatchRunner;

  //크론 표현식으로 매 시각 5분에 실행되도록 처리
  @Scheduled(cron = "0 5 * * * *", zone = "Asia/Seoul")
  public void runHourly() {
    try {
      articleScrapeBatchRunner.runNow();
    } catch (JobExecutionException e) {
      log.error("[ARTICLE_SCHEDULER] hourly batch failed", e);
    }
  }
}