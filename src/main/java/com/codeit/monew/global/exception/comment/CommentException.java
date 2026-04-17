package com.codeit.monew.global.exception.comment;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.MonewException;

public abstract class CommentException extends MonewException {

  protected CommentException(ErrorCode errorCode, String key, Object value) {
    super(errorCode);
    addDetail(key, value);
  }
}
