package com.codeit.monew.global.exception.article;

import com.codeit.monew.global.exception.ErrorCode;

public class ArticleFileReadFailedException extends ArticleException {

  public ArticleFileReadFailedException(Throwable cause) {
    super(ErrorCode.ARTICLE_FILE_READ_FAILED, "message", "File read failed.", cause);
  }
}
