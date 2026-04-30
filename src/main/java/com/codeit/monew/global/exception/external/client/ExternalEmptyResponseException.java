package com.codeit.monew.global.exception.external.client;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.external.ExternalApiException;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;

public class ExternalEmptyResponseException extends ExternalApiException {

  public ExternalEmptyResponseException(NewsSourceUrl source, String url) {
    super(ErrorCode.EXTERNAL_EMPTY_RESPONSE, source, url, null);
  }
}
