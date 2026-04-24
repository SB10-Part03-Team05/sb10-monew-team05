package com.codeit.monew.global.exception.external;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.MonewException;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;

public class ExternalInvalidXmlException extends MonewException {

  public ExternalInvalidXmlException(NewsSourceUrl source, String reason, Integer xmlLength) {
    super(ErrorCode.EXTERNAL_INVALID_XML);
    addDetail("source", source);
    addDetail("reason", reason);
    addDetail("xmlLength", xmlLength);
  }

  public ExternalInvalidXmlException(NewsSourceUrl source, String reason, Throwable cause) {
    super(ErrorCode.EXTERNAL_INVALID_XML, cause);
    addDetail("source", source);
    addDetail("reason", reason);
  }
}
