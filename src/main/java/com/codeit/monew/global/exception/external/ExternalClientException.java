package com.codeit.monew.global.exception.external;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;

public class ExternalClientException extends ExternalApiException {

  public ExternalClientException(NewsSourceUrl source, String url, int statusCode,
      Throwable cause) {
    super(ErrorCode.EXTERNAL_CLIENT_ERROR, source, url, statusCode, false, cause);
  }
}
