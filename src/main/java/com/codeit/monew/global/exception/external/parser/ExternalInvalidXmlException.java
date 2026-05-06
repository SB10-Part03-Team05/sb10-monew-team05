package com.codeit.monew.global.exception.external.parser;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.external.ExternalApiException;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;

public class ExternalInvalidXmlException extends ExternalApiException {

  public ExternalInvalidXmlException(NewsSourceUrl source, Throwable cause) {
    super(ErrorCode.EXTERNAL_INVALID_XML, source, null, null, cause);
    addDetail("reason", "invalid_xml");
  }
}
