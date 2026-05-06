package com.codeit.monew.global.exception.aws;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.MonewException;

public abstract class AwsException extends MonewException {

  protected AwsException(ErrorCode errorCode, String field, Object value, Throwable cause) {
    super(errorCode, cause);
    addDetail(field, value);
  }
}
