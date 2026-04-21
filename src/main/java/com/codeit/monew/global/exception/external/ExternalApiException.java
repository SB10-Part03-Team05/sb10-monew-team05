package com.codeit.monew.global.exception.external;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.MonewException;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import lombok.Getter;

@Getter
public abstract class ExternalApiException extends MonewException {

  private final NewsSourceUrl source;
  private final String url;
  private final Integer statusCode;
  private final boolean retryable;

  protected ExternalApiException(
      ErrorCode errorCode,
      NewsSourceUrl source,
      String url,
      Integer statusCode,
      boolean retryable,
      Throwable cause
  ) {
    super(errorCode, cause);
    this.source = source;
    this.url = url;
    this.statusCode = statusCode;
    this.retryable = retryable;
    addDetail("source", source);
    addDetail("url", url);
    addDetail("statusCode", statusCode);
    addDetail("retryable", retryable);
  }
}
