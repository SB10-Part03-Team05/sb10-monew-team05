package com.codeit.monew.global.exception.external;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import lombok.Getter;

@Getter
public class ExternalRateLimitException extends ExternalApiException {

  private final boolean retryNextBatch;

  public ExternalRateLimitException(
      NewsSourceUrl source,
      String url,
      boolean retryableNow,
      boolean retryNextBatch,
      Throwable cause
  ) {
    super(ErrorCode.EXTERNAL_RATE_LIMITED, source, url, 429, retryableNow, cause);
    this.retryNextBatch = retryNextBatch;
    addDetail("retryNextBatch", retryNextBatch);
  }
}
