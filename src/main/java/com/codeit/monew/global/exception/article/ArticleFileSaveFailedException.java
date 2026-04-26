package com.codeit.monew.global.exception.article;

import com.codeit.monew.global.exception.ErrorCode;

public class ArticleFileSaveFailedException extends ArticleException {

  public ArticleFileSaveFailedException(Throwable cause) {
    super(ErrorCode.ARTICLE_FILE_SAVE_FAILED, "message", "File save failed.", cause);
  }
}
