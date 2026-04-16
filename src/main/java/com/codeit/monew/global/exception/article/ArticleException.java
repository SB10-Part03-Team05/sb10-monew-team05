package com.codeit.monew.global.exception.article;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.MonewException;

public abstract class ArticleException extends MonewException {

  protected ArticleException(ErrorCode errorCode, String key, Object value) {
    super(errorCode);
    addDetail(key, value);
  }

}
