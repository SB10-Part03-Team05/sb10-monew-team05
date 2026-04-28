package com.codeit.monew.global.exception.external;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.MonewException;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public abstract class ExternalApiException extends MonewException {

  private final NewsSourceUrl source;
  private final String url;
  private final HttpStatusCode statusCode;

  protected ExternalApiException(ErrorCode errorCode, NewsSourceUrl source, String url,
      HttpStatusCode statusCode, Throwable cause
  ) {
    super(errorCode, cause);
    this.source = source;
    this.url = url;
    this.statusCode = statusCode;
    addDetail("source", source);
    addDetail("url", url);
    addDetail("statusCode", statusCode);
  }

  protected ExternalApiException(ErrorCode errorCode, NewsSourceUrl source, String url,
      HttpStatusCode statusCode
  ) {
    super(errorCode);
    this.source = source;
    this.url = url;
    this.statusCode = statusCode;
    addDetail("source", source);
    addDetail("url", url);
    addDetail("statusCode", statusCode);
  }
}
