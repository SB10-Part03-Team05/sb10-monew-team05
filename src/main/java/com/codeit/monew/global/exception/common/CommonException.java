package com.codeit.monew.global.exception.common;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.MonewException;

public abstract class CommonException extends MonewException {

  protected CommonException(ErrorCode errorCode, String field, Object value) {
    super(errorCode);
    addDetail(field, value);
  }

  protected CommonException(ErrorCode errorCode, String field1, Object value1, String field2,
      Object value2) {
    super(errorCode);
    addDetail(field1, value1);
    addDetail(field2, value2);
  }
}
