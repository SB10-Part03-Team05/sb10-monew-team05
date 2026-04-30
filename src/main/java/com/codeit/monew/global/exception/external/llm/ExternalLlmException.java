package com.codeit.monew.global.exception.external.llm;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.MonewException;

public abstract class ExternalLlmException extends MonewException {

  protected ExternalLlmException(ErrorCode errorCode) {
    super(errorCode);
  }

  protected ExternalLlmException(ErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}
