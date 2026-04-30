package com.codeit.monew.global.exception.external.crawl;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.MonewException;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;

public class ExternalArticleCrawlException extends MonewException {

  public ExternalArticleCrawlException(NewsSourceUrl source, String articleUrl, Throwable cause) {
    super(ErrorCode.EXTERNAL_ARTICLE_CRAWL_ERROR, cause);
    addDetail("source", source);
    addDetail("articleUrl", articleUrl);
  }
}
