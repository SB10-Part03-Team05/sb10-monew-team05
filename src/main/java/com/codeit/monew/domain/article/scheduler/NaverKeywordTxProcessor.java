package com.codeit.monew.domain.article.scheduler;

import com.codeit.monew.domain.article.service.ArticleScrapeService;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NaverKeywordTxProcessor {

  private final ArticleScrapeService articleScrapeService;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ArticleScrapeResult processOneKeyword(String keyword) {
    return articleScrapeService.scrapeAndSave(NewsSourceUrl.NAVER, keyword);
  }
}