package com.codeit.monew.domain.article.scheduler;

import com.codeit.monew.domain.article.service.ArticleScrapeService;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RssSourceTxProcessor {

  private final ArticleScrapeService articleScrapeService;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ArticleScrapeResult processOneSource(NewsSourceUrl source) {
    return articleScrapeService.scrapeAndSave(source, null);
  }
}