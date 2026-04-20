package com.codeit.monew.domain.article.repository.impl;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.dto.response.ArticleDto;
import com.codeit.monew.domain.article.dto.response.CursorPageResponseArticleDto;
import com.codeit.monew.domain.article.entity.QArticle;
import com.codeit.monew.domain.article.entity.QArticleInterest;
import com.codeit.monew.domain.article.entity.QArticleViewHistory;
import com.codeit.monew.domain.article.entity.type.ArticleDirection;
import com.codeit.monew.domain.article.entity.type.ArticleOrderBy;
import com.codeit.monew.domain.article.repository.ArticleQueryRepository;
import com.codeit.monew.domain.comment.entity.QComment;
import com.codeit.monew.global.exception.common.InvalidParameterException;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;

import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArticleQueryRepositoryImpl implements ArticleQueryRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public CursorPageResponseArticleDto searchArticleList(ArticleSearchRequest request,
      UUID requestUserId) {
    // 기본 인스턴스 사용
    QArticle article = QArticle.article;
    QComment comment = QComment.comment;
    QArticleInterest articleInterest = QArticleInterest.articleInterest;
    // 별칭 직접 지정
    QArticleViewHistory articleViewAll = new QArticleViewHistory("articleViewAll"); // 조회 수
    QArticleViewHistory articleViewMe = new QArticleViewHistory("articleViewMe"); // 사용자 조회 여부

    NumberExpression<Long> commentCountExpression = comment.id.countDistinct();
    NumberExpression<Long> viewCountExpression = articleViewAll.id.countDistinct();
    Pageable pageable = PageRequest.of(0, request.getLimit());

    // 정렬 기준에 따라 분기(각자 참고하는 기준이 다름)
    // publishDate는 article table, viewCount는 article_histories table, commentCount는 comment table
    Slice<ArticleDto> articleDtoSlice =
        switch (request.getOrderBy()) {
          case publishDate ->
              searchArticleListByPublishDate(request, requestUserId, article, comment,
                  articleViewAll, articleViewMe, articleInterest, pageable);
          case commentCount -> {
            Long normalizedCursor =
                request.getCursor() == null ? null : parserLong(request.getCursor());
            yield searchArticleListByCommentCount(request, requestUserId, article, comment,
                articleViewAll, articleViewMe, articleInterest, normalizedCursor,
                commentCountExpression, pageable);
          }
          case viewCount -> {
            Long normalizedCursor =
                request.getCursor() == null ? null : parserLong(request.getCursor());
            yield searchArticleListByViewCount(request, requestUserId, article, comment,
                articleViewAll, articleViewMe, articleInterest, normalizedCursor,
                viewCountExpression, pageable);
          }
        };

    // 전체 요소 수
    Long totalElement = findTotalElement(request, article, articleInterest);

    // 마지막 ArticleDto 찾기
    List<ArticleDto> sliceContent = articleDtoSlice.getContent();
    ArticleDto lastArticleDto = !sliceContent.isEmpty()
        ? sliceContent.get(sliceContent.size() - 1)
        : null;

    // nextCursor, afterCursor
    String nextCursor = null;
    Instant after = null;
    if (articleDtoSlice.hasNext() && lastArticleDto != null) {
      nextCursor = findNextCursor(lastArticleDto, request.getOrderBy());
      after = findCreatedAtById(article, lastArticleDto.id());
    }

    return new CursorPageResponseArticleDto(
        articleDtoSlice.getContent(),
        nextCursor,
        after,
        articleDtoSlice.getNumberOfElements(),
        totalElement,
        articleDtoSlice.hasNext()
    );
  }

  // orderBy가 publishDate일 때
  private Slice<ArticleDto> searchArticleListByPublishDate(ArticleSearchRequest request,
      UUID requestUserId, QArticle article, QComment comment, QArticleViewHistory articleViewAll,
      QArticleViewHistory articleViewMe, QArticleInterest articleInterest, Pageable pageable) {

    Instant normalizedCursor =
        request.getCursor() == null ? null : parserInstant(request.getCursor());

    List<ArticleDto> content = searchQueryByPublishDate(request, requestUserId, article, comment,
        articleViewAll, articleViewMe, articleInterest, normalizedCursor);

    // Slice 생성
    return toSlice(content, pageable);
  }

  // orderBy가 commentCount일 때
  private Slice<ArticleDto> searchArticleListByCommentCount(ArticleSearchRequest request,
      UUID requestUserId, QArticle article, QComment comment, QArticleViewHistory articleViewAll,
      QArticleViewHistory articleViewMe, QArticleInterest articleInterest, Long normalizedCursor,
      NumberExpression<Long> commentCountExpression, Pageable pageable) {

    List<ArticleDto> content = searchQueryByCount(request, requestUserId, article, comment,
        articleViewAll, articleViewMe, articleInterest, normalizedCursor, commentCountExpression);

    // Slice 생성
    return toSlice(content, pageable);

  }

  // orderBy가 viewCount일 때
  private Slice<ArticleDto> searchArticleListByViewCount(ArticleSearchRequest request,
      UUID requestUserId, QArticle article, QComment comment, QArticleViewHistory articleViewAll,
      QArticleViewHistory articleViewMe, QArticleInterest articleInterest, Long normalizedCursor,
      NumberExpression<Long> viewCountExpression, Pageable pageable) {

    List<ArticleDto> content = searchQueryByCount(request, requestUserId, article, comment,
        articleViewAll, articleViewMe, articleInterest, normalizedCursor, viewCountExpression);

    // Slice 생성
    return toSlice(content, pageable);
  }

  // orderBy = publishDate
  List<ArticleDto> searchQueryByPublishDate(ArticleSearchRequest request, UUID requestUserId,
      QArticle article, QComment comment, QArticleViewHistory articleViewAll,
      QArticleViewHistory articleViewMe, QArticleInterest articleInterest,
      Instant normalizedCursor) {

    return baseQuery(request, requestUserId, article, comment, articleViewAll, articleViewMe,
        articleInterest)
        .where(
            publishDateCursorCondition(article, request.getDirection(), normalizedCursor,
                request.getAfter())
        )
        .orderBy(
            request.getDirection() == ArticleDirection.DESC
                ? article.publishDate.desc()
                : article.publishDate.asc(),
            request.getDirection() == ArticleDirection.DESC
                ? article.createdAt.desc()
                : article.createdAt.asc(),
            request.getDirection() == ArticleDirection.DESC
                ? article.id.desc()
                : article.id.asc()
        )
        .limit(request.getLimit() + 1)
        .fetch();
  }

  // orderBy = commentCount || viewCount
  List<ArticleDto> searchQueryByCount(ArticleSearchRequest request, UUID requestUserId,
      QArticle article, QComment comment, QArticleViewHistory articleViewAll,
      QArticleViewHistory articleViewMe, QArticleInterest articleInterest,
      Long normalizedCursor, NumberExpression<Long> countExpression) {

    return baseQuery(request, requestUserId, article, comment, articleViewAll, articleViewMe,
        articleInterest)
        .having(
            countCursorCondition(article, request.getDirection(), normalizedCursor,
                request.getAfter(), countExpression)
        )
        .orderBy(
            request.getDirection() == ArticleDirection.DESC
                ? countExpression.desc()
                : countExpression.asc(),
            request.getDirection() == ArticleDirection.DESC
                ? article.createdAt.desc()
                : article.createdAt.asc(),
            request.getDirection() == ArticleDirection.DESC
                ? article.id.desc()
                : article.id.asc()
        )
        .limit(request.getLimit() + 1)
        .fetch();
  }

  private JPAQuery<ArticleDto> baseQuery(ArticleSearchRequest request, UUID requestUserId,
      QArticle article, QComment comment, QArticleViewHistory articleViewAll,
      QArticleViewHistory articleViewMe, QArticleInterest articleInterest) {

    return queryFactory.select(Projections.constructor(
            ArticleDto.class,
            article.id,
            article.source,
            article.sourceUrl,
            article.title,
            article.publishDate,
            article.summary,
            comment.id.countDistinct(), // 댓글 수 집계
            articleViewAll.id.countDistinct(), // 조회 수 집계
            articleViewMe.id.countDistinct().gt(0L) // 조회 여부(1이상이면 true)
        ))
        .from(article)
        .leftJoin(articleInterest).on(
            articleInterest.article.eq(article)
        )
        .leftJoin(comment).on(
            comment.article.eq(article),
            comment.deletedAt.isNull() // 논리 삭제된 것은 제외
        )
        .leftJoin(articleViewAll).on(
            articleViewAll.article.eq(article)
        )
        .leftJoin(articleViewMe).on(
            articleViewMe.article.eq(article),
            articleViewMe.user.id.eq(requestUserId) // 조회 여부
        )
        .where(
            commonWhere(request, article, articleInterest)
        )
        .groupBy(article.id);
  }

  private BooleanExpression[] commonWhere(ArticleSearchRequest request, QArticle article,
      QArticleInterest articleInterest) {

    return new BooleanExpression[]{
        article.deletedAt.isNull(),
        keywordContains(article, request.getKeyword()),
        interestIdEq(articleInterest, request.getInterestId()),
        sourceIn(article, request.getSourceIn()),
        publishDateGoe(article, request.getPublishDateFrom()),
        publishDateLoe(article, request.getPublishDateTo())
    };
  }

  private Instant parserInstant(String cursor) {
    if (cursor == null) {
      return null;
    }

    try {
      return Instant.parse(cursor);
    } catch (DateTimeParseException e) {
      throw new InvalidParameterException("cursor", cursor);
    }
  }

  private Long parserLong(String cursor) {
    if (cursor == null) {
      return null;
    }

    try {
      return Long.parseLong(cursor);
    } catch (NumberFormatException e) {
      throw new InvalidParameterException("cursor", cursor);
    }
  }

  private BooleanExpression keywordContains(QArticle article, String keyword) {
    return keyword != null
        ? article.title.contains(keyword).or(article.summary.contains(keyword))
        : null;
  }

  private BooleanExpression interestIdEq(QArticleInterest articleInterest, UUID interestId) {
    return interestId != null
        ? articleInterest.interest.id.eq(interestId)
        : null;
  }

  private BooleanExpression sourceIn(QArticle article, List<ArticleSource> sourceIn) {
    return sourceIn != null
        ? article.source.in(sourceIn)
        : null;
  }

  // `goe` -> `>=` 이상
  private BooleanExpression publishDateGoe(QArticle article, Instant publishDate) {
    return publishDate != null ? article.publishDate.goe(publishDate) : null;
  }

  // `loe` -> `<=` 이하
  private BooleanExpression publishDateLoe(QArticle article, Instant publishDate) {
    return publishDate != null ? article.publishDate.loe(publishDate) : null;
  }

  // orderBy가 publishDate일 때 커서 조건
  private BooleanExpression publishDateCursorCondition(QArticle article, ArticleDirection direction,
      Instant cursor, Instant after) {

    if (cursor == null || after == null) {
      return null;
    }

    // `lt` -> `<` 미만
    if (direction == ArticleDirection.DESC) {
      return article.publishDate.lt(cursor)
          .or(article.publishDate.eq(cursor).and(article.createdAt.lt(after)));
    }

    // `gt` -> `>` 초과
    return article.publishDate.gt(cursor)
        .or(article.publishDate.eq(cursor).and(article.createdAt.gt(after)));
  }

  // orderBy가 commentCount/viewCount일 때 커서 조건
  private BooleanExpression countCursorCondition(QArticle article,
      ArticleDirection direction, Long cursor, Instant after,
      NumberExpression<Long> countExpression) {

    if (cursor == null || after == null) {
      return null;
    }

    // `lt` -> `<` 미만
    if (direction == ArticleDirection.DESC) {
      return countExpression.lt(cursor)
          .or(countExpression.eq(cursor).and(article.createdAt.lt(after)));
    }

    // `gt` -> `>` 초과
    return countExpression.gt(cursor)
        .or(countExpression.eq(cursor).and(article.createdAt.gt(after)));
  }

  // Slice 생성 메서드
  private Slice<ArticleDto> toSlice(List<ArticleDto> content, Pageable pageable) {
    boolean hasNext = content.size() > pageable.getPageSize(); // 11 > 10

    if (hasNext) {
      content.remove(pageable.getPageSize());
    }

    return new SliceImpl<>(content, pageable, hasNext);
  }

  // nextCursor(다음 페이지 커서) 찾기
  private String findNextCursor(ArticleDto lastArticleDto, ArticleOrderBy orderBy) {
    return switch (orderBy) {
      case publishDate -> lastArticleDto.publishDate().toString();
      case commentCount -> String.valueOf(lastArticleDto.commentCount());
      case viewCount -> String.valueOf(lastArticleDto.viewCount());
    };
  }

  // nextAfter(다음 보조 커서) 찾기
  private Instant findCreatedAtById(QArticle article, UUID articleId) {
    return queryFactory
        .select(article.createdAt)
        .from(article)
        .where(
            article.id.eq(articleId),
            article.deletedAt.isNull()
        )
        .fetchOne();
  }

  // totalElement(총 요소 수) 찾기
  private Long findTotalElement(ArticleSearchRequest request, QArticle article,
      QArticleInterest articleInterest) {
    return queryFactory
        .select(article.id.countDistinct())
        .from(article)
        .leftJoin(articleInterest).on(
            articleInterest.article.eq(article)
        )
        .where(
            commonWhere(request, article, articleInterest)
        )
        .fetchOne();
  }
}
