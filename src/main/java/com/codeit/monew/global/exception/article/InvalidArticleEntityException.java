package com.codeit.monew.global.exception.article;

import com.codeit.monew.global.exception.ErrorCode;

public class InvalidArticleEntityException extends ArticleException {

  public InvalidArticleEntityException(String field, String reason) {
    super(ErrorCode.INVALID_ARTICLE_ENTITY, "field", field);
    addDetail("reason", reason);
  }

  public InvalidArticleEntityException(String field, String reason, int maxLength, int actualLength) {
    this(field, reason);
    addDetail("maxLength", maxLength);
    addDetail("actualLength", actualLength);
  }
}
