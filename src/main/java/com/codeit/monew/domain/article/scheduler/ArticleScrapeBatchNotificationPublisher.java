package com.codeit.monew.domain.article.scheduler;

import com.codeit.monew.domain.notification.event.BulkArticleRegisteredEvent;
import com.codeit.monew.domain.notification.event.BulkArticleRegisteredEvent.InterestArticleCount;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleScrapeBatchNotificationPublisher {

  private final ApplicationEventPublisher eventPublisher;

  public void publishIfNeeded(ArticleScrapeResult totalResult) {
    if (totalResult == null || totalResult.totalSavedCount() <= 0) {
      return;
    }

    List<InterestArticleCount> interestCounts = totalResult.results().entrySet().stream()
        .map(entry -> new InterestArticleCount(
            entry.getKey(),
            entry.getValue().name(),
            entry.getValue().count()
        ))
        .toList();

    log.info("[ARTICLE_BATCH_JOB] publish notification event. totalSavedCount={}",
        totalResult.totalSavedCount());
    
    eventPublisher.publishEvent(new BulkArticleRegisteredEvent(interestCounts));
  }
}
