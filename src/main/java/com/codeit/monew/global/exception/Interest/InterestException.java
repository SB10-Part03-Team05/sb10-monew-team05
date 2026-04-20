package com.codeit.monew.global.exception.Interest;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.MonewException;

public class InterestException extends MonewException {

  protected InterestException(ErrorCode errorCode, String key, Object value) {
    super(errorCode);
    addDetail(key, value);
  }
}
