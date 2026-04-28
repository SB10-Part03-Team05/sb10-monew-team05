package com.codeit.monew.global.exception.external.client;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.external.ExternalApiException;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import org.springframework.http.HttpStatusCode;

public class ExternalRateLimitException extends ExternalApiException {

  public ExternalRateLimitException(NewsSourceUrl source, String url, HttpStatusCode statusCode,
      Throwable cause) {
    super(ErrorCode.EXTERNAL_RATE_LIMITED, source, url, statusCode, cause);
  }
}
