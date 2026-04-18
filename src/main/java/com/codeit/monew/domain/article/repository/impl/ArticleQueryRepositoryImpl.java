package com.codeit.monew.domain.article.repository.impl;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.dto.response.CursorPageResponseArticleDto;
import com.codeit.monew.domain.article.repository.ArticleQueryRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArticleQueryRepositoryImpl implements ArticleQueryRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public CursorPageResponseArticleDto searchArticleList(ArticleSearchRequest request,
      UUID requestUserId) {
    // 정렬 기준에 따라 분기(각자 참고하는 기준이 다름)
    // publishDate는 article table, viewCount는 article_histories table, commentCount는 comment table
    return switch (request.getOrderBy()) {
      case publishDate -> searchArticleListByPublishDate(request, requestUserId);
      case viewCount -> searchArticleListByViewCount(request, requestUserId);
      case commentCount -> searchArticleListByCommentCount(request, requestUserId);
    };
  }

  private CursorPageResponseArticleDto searchArticleListByPublishDate(ArticleSearchRequest request,
      UUID requestUserId) {
    Instant normalizedCursor =
        request.getCursor() == null ? null : parserInstant(request.getCursor());

    return null;
  }

  private CursorPageResponseArticleDto searchArticleListByViewCount(ArticleSearchRequest request,
      UUID requestUserId) {
    Long normalizedCursor = request.getCursor() == null ? null : parserLong(request.getCursor());

    return null;
  }

  private CursorPageResponseArticleDto searchArticleListByCommentCount(ArticleSearchRequest request,
      UUID requestUserId) {
    Long normalizedCursor = request.getCursor() == null ? null : parserLong(request.getCursor());

    return null;
  }

  private Instant parserInstant(String cursor) {
    if (cursor == null) {
      return null;
    }
    return Instant.parse(cursor);
  }

  private Long parserLong(String cursor) {
    if (cursor == null) {
      return null;
    }
    return Long.parseLong(cursor);
  }
}
