package com.codeit.monew.global.exception.user;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.MonewException;

public abstract class UserException extends MonewException {

  protected UserException(ErrorCode errorCode, String key, Object value) {
    super(errorCode);
    addDetail(key, value);
  }
}
