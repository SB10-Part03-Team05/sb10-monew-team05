package com.codeit.monew.global.exception.external;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;

public class ExternalServerException extends ExternalApiException {

  public ExternalServerException(NewsSourceUrl source, String url, int statusCode,
      Throwable cause) {
    super(ErrorCode.EXTERNAL_SERVER_ERROR, source, url, statusCode, true, cause);
  }
}
