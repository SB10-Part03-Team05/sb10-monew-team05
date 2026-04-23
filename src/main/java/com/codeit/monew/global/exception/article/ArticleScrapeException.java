package com.codeit.monew.global.exception.article;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;

public class ArticleScrapeException extends ArticleException {

  public ArticleScrapeException(NewsSourceUrl source, String query, String stage, Throwable cause) {
    super(ErrorCode.ARTICLE_SCRAPE_FAILED, "source", source, cause);
    addDetail("query", query);
    addDetail("stage", stage);
  }
}
