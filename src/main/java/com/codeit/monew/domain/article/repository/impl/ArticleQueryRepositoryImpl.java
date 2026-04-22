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

import java.time.LocalDateTime;
import java.time.ZoneId;
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
          case publishDate -> {
            PublishDateCursor cursor = parserPublishDateCursor(request.getCursor());

            yield searchArticleListByPublishDate(request, requestUserId, article, comment,
                articleViewAll, articleViewMe, articleInterest, cursor, pageable);
          }
          case commentCount -> {
            CountCursor cursor = parserCountCursor(request.getCursor());

            yield searchArticleListByCommentCount(request, requestUserId, article, comment,
                articleViewAll, articleViewMe, articleInterest, cursor,
                commentCountExpression, pageable);
          }
          case viewCount -> {
            CountCursor cursor = parserCountCursor(request.getCursor());

            yield searchArticleListByViewCount(request, requestUserId, article, comment,
                articleViewAll, articleViewMe, articleInterest, cursor,
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
      Instant createdAt = findCreatedAtById(article, lastArticleDto.id());
      nextCursor = findNextCursor(lastArticleDto, request.getOrderBy(), createdAt);
      after = createdAt;
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

  private record PublishDateCursor(
      Instant publishDate,
      Instant createdAt,
      UUID id
  ) {

  }

  private record CountCursor(
      Long count,
      Instant createdAt,
      UUID id
  ) {

  }

  private PublishDateCursor parserPublishDateCursor(String cursor) {
    if (cursor == null) {
      return null;
    }

    String[] cursorParts = cursor.split("\\|", -1);
    if (cursorParts.length != 3) {
      throw new InvalidParameterException("cursor", cursor);
    }

    try {
      return new PublishDateCursor(
          parserInstant(cursorParts[0]),
          parserInstant(cursorParts[1]),
          parserUUID(cursorParts[2])
      );
    } catch (DateTimeParseException | IllegalArgumentException e) {
      throw new InvalidParameterException("cursor", cursor);
    }
  }

  private CountCursor parserCountCursor(String cursor) {
    if (cursor == null) {
      return null;
    }

    String[] cursorParts = cursor.split("\\|", -1);
    if (cursorParts.length != 3) {
      throw new InvalidParameterException("cursor", cursor);
    }

    try {
      return new CountCursor(
          parserLong(cursorParts[0]),
          parserInstant(cursorParts[1]),
          parserUUID(cursorParts[2])
      );
    } catch (DateTimeParseException | IllegalArgumentException e) {
      throw new InvalidParameterException("cursor", cursor);
    }
  }

  private Instant parserInstant(String stringInstant) {
    if (stringInstant == null) {
      return null;
    }

    return Instant.parse(stringInstant);
  }

  private UUID parserUUID(String stringId) {
    if (stringId == null) {
      return null;
    }

    return UUID.fromString(stringId);
  }

  private Long parserLong(String stringLong) {
    if (stringLong == null) {
      return null;
    }

    return Long.parseLong(stringLong);
  }

  // orderBy가 publishDate일 때
  private Slice<ArticleDto> searchArticleListByPublishDate(ArticleSearchRequest request,
      UUID requestUserId, QArticle article, QComment comment, QArticleViewHistory articleViewAll,
      QArticleViewHistory articleViewMe, QArticleInterest articleInterest,
      ArticleQueryRepositoryImpl.PublishDateCursor cursor,
      Pageable pageable) {

    List<ArticleDto> content = searchQueryByPublishDate(request, requestUserId, article, comment,
        articleViewAll, articleViewMe, articleInterest, cursor);

    // Slice 생성
    return toSlice(content, pageable);
  }

  // orderBy가 commentCount일 때
  private Slice<ArticleDto> searchArticleListByCommentCount(ArticleSearchRequest request,
      UUID requestUserId, QArticle article, QComment comment, QArticleViewHistory articleViewAll,
      QArticleViewHistory articleViewMe, QArticleInterest articleInterest, CountCursor cursor,
      NumberExpression<Long> commentCountExpression, Pageable pageable) {

    List<ArticleDto> content = searchQueryByCount(request, requestUserId, article, comment,
        articleViewAll, articleViewMe, articleInterest, cursor, commentCountExpression);

    // Slice 생성
    return toSlice(content, pageable);

  }

  // orderBy가 viewCount일 때
  private Slice<ArticleDto> searchArticleListByViewCount(ArticleSearchRequest request,
      UUID requestUserId, QArticle article, QComment comment, QArticleViewHistory articleViewAll,
      QArticleViewHistory articleViewMe, QArticleInterest articleInterest, CountCursor cursor,
      NumberExpression<Long> viewCountExpression, Pageable pageable) {

    List<ArticleDto> content = searchQueryByCount(request, requestUserId, article, comment,
        articleViewAll, articleViewMe, articleInterest, cursor, viewCountExpression);

    // Slice 생성
    return toSlice(content, pageable);
  }

  // orderBy = publishDate
  List<ArticleDto> searchQueryByPublishDate(ArticleSearchRequest request, UUID requestUserId,
      QArticle article, QComment comment, QArticleViewHistory articleViewAll,
      QArticleViewHistory articleViewMe, QArticleInterest articleInterest,
      PublishDateCursor cursor) {

    return baseQuery(request, requestUserId, article, comment, articleViewAll, articleViewMe,
        articleInterest)
        .where(
            publishDateCursorCondition(article, request.getDirection(), cursor,
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
      CountCursor cursor, NumberExpression<Long> countExpression) {

    return baseQuery(request, requestUserId, article, comment, articleViewAll, articleViewMe,
        articleInterest)
        .having(
            countCursorCondition(article, request.getDirection(), cursor,
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

    ZoneId zoneId = ZoneId.of("Asia/Seoul");

    LocalDateTime publishDateFrom = request.getPublishDateFrom();
    LocalDateTime publishDateTo = request.getPublishDateTo();

    Instant fromInstant = publishDateFrom != null
        ? publishDateFrom.atZone(zoneId).toInstant()
        : null;
    Instant toInstant = publishDateTo != null
        ? publishDateTo.atZone(zoneId).toInstant()
        : null;

    return new BooleanExpression[]{
        article.deletedAt.isNull(),
        keywordContains(article, request.getKeyword()),
        interestIdEq(articleInterest, request.getInterestId()),
        sourceIn(article, request.getSourceIn()),
        publishDateGoe(article, fromInstant),
        publishDateLoe(article, toInstant)
    };
  }

  private BooleanExpression keywordContains(QArticle article, String keyword) {
    // ""일 경우를 `null` 을 가지게 하기 위해 `isBlank()` 조건 추가
    return keyword != null && !keyword.isBlank()
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
      PublishDateCursor cursor, Instant after) {

    if (cursor == null) {
      return null;
    }

    // `lt` -> `<` 미만
    if (direction == ArticleDirection.DESC) {
      return article.publishDate.lt(cursor.publishDate())
          .or(article.publishDate.eq(cursor.publishDate())
              .and(article.createdAt.lt(cursor.createdAt())))
          .or(article.publishDate.eq(cursor.publishDate())
              .and(article.createdAt.eq(cursor.createdAt()))
              .and(article.id.lt(cursor.id())));
    }

    // `gt` -> `>` 초과
    return article.publishDate.gt(cursor.publishDate())
        .or(article.publishDate.eq(cursor.publishDate())
            .and(article.createdAt.gt(cursor.createdAt())))
        .or(article.publishDate.eq(cursor.publishDate())
            .and(article.createdAt.eq(cursor.createdAt()))
            .and(article.id.gt(cursor.id())));
  }

  // orderBy가 commentCount/viewCount일 때 커서 조건
  private BooleanExpression countCursorCondition(QArticle article, ArticleDirection direction,
      CountCursor cursor, Instant after, NumberExpression<Long> countExpression) {

    if (cursor == null) {
      return null;
    }

    // `lt` -> `<` 미만
    if (direction == ArticleDirection.DESC) {
      return countExpression.lt(cursor.count())
          .or(countExpression.eq(cursor.count())
              .and(article.createdAt.lt(cursor.createdAt())))
          .or(countExpression.eq(cursor.count())
              .and(article.createdAt.eq(cursor.createdAt()))
              .and(article.id.lt(cursor.id())));
    }

    // `gt` -> `>` 초과
    return countExpression.gt(cursor.count())
        .or(countExpression.eq(cursor.count())
            .and(article.createdAt.gt(cursor.createdAt())))
        .or(countExpression.eq(cursor.count())
            .and(article.createdAt.eq(cursor.createdAt()))
            .and(article.id.gt(cursor.id())));
  }

  // Slice 생성 메서드
  private Slice<ArticleDto> toSlice(List<ArticleDto> content, Pageable pageable) {
    boolean hasNext = content.size() > pageable.getPageSize(); // 11 > 10

    if (hasNext) {
      content.remove(pageable.getPageSize());
    }

    return new SliceImpl<>(content, pageable, hasNext);
  }

  // nextCursor(다음 페이지 커서) 조합
  private String findNextCursor(ArticleDto lastArticleDto, ArticleOrderBy orderBy,
      Instant createdAt) {
    UUID lastArticleId = lastArticleDto.id();

    return switch (orderBy) {
      // "publishDate|createdAt|articleId"
      case publishDate -> String.join(
          "|",
          lastArticleDto.publishDate().toString(),
          createdAt.toString(),
          lastArticleId.toString()
      );
      // "commentCount|createdAt|articleId"
      case commentCount -> String.join(
          "|",
          String.valueOf(lastArticleDto.commentCount()),
          createdAt.toString(),
          lastArticleId.toString()
      );
      // "viewCount|createdAt|articleId"
      case viewCount -> String.join(
          "|",
          String.valueOf(lastArticleDto.viewCount()),
          createdAt.toString(),
          lastArticleId.toString()
      );
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
