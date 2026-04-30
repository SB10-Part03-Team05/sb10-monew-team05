package com.codeit.monew.global.exception.external.client;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.external.ExternalApiException;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;

public class ExternalNetworkException extends ExternalApiException {

  public ExternalNetworkException(NewsSourceUrl source, String url, Throwable cause) {
    super(ErrorCode.EXTERNAL_NETWORK_ERROR, source, url, null, cause);
  }
}
