package com.codeit.monew.global.exception.article;

import com.codeit.monew.global.exception.ErrorCode;
import java.util.UUID;

public class ArticleNotFoundException extends ArticleException {

  public ArticleNotFoundException(UUID articleId) {
    super(ErrorCode.ARTICLE_NOT_FOUND, "articleId", articleId);
  }
}
