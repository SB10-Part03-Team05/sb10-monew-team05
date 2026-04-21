package com.codeit.monew.domain.article.repository;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.dto.response.CursorPageResponseArticleDto;
import java.util.UUID;

public interface ArticleQueryRepository {

  CursorPageResponseArticleDto searchArticleList(ArticleSearchRequest request, UUID requestUserId);
}
