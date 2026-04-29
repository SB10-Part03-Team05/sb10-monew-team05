package com.codeit.monew.global.exception.external.llm;

import com.codeit.monew.global.exception.ErrorCode;

public class ExternalLlmInvalidInputException extends ExternalLlmException {

  public ExternalLlmInvalidInputException(String provider, String field) {
    super(ErrorCode.EXTERNAL_LLM_INVALID_INPUT);
    addDetail("provider", provider);
    addDetail("field", field);
    addDetail("reason", field + " is null or blank");
  }
}
