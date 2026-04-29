package com.codeit.monew.global.exception.external.llm;

import com.codeit.monew.global.exception.ErrorCode;

public class ExternalLlmInvalidInputException extends ExternalLlmException {

  public ExternalLlmInvalidInputException(String provider, String field, Object value) {
    super(ErrorCode.EXTERNAL_LLM_INVALID_INPUT);
    addDetail("provider", provider);
    addDetail("field", field);
    addDetail("value", value);
  }
}
