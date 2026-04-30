package com.codeit.monew.global.exception.external.parser;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.MonewException;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;

public class EmptyXmlInputException extends MonewException {

  public EmptyXmlInputException(NewsSourceUrl source) {
    super(ErrorCode.EMPTY_XML_INPUT);
    addDetail("source", source);
    addDetail("reason", "empty_or_blank_xml");
  }
}
