package com.codeit.monew.domain.comment.repository.impl;

import static com.codeit.monew.domain.comment.entity.QComment.comment;

import com.codeit.monew.domain.comment.dto.CommentCursorRequest;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.repository.CommentQueryRepository;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentQueryRepositoryImpl implements CommentQueryRepository {
  private final JPAQueryFactory queryFactory;

  @Override
  public List<Comment> findCommentsByCursor(UUID articleId, CommentCursorRequest request) {
    return queryFactory
        .selectFrom(comment)
        .leftJoin(comment.user).fetchJoin() // 사용자(닉네임) 정보를 한 번의 쿼리로 다 끌고 옴 (N+1 방어)
        .where(
            comment.article.id.eq(articleId), // 해당 기사의 댓글만 필터링
            comment.deletedAt.isNull(), // 논리 삭제된 댓글 제외
            cursorCondition(request.orderBy(), request.direction(), request.cursor(), request.after())
        )
        .orderBy(createOrderSpecifier(request.orderBy(), request.direction()))
        .limit(request.limit() + 1) // hasNext 판별을 위해 요청한 개수보다 1개 더 가져옴
        .fetch();
  }

  // 동적 커서 조건 생성기
  private BooleanExpression cursorCondition(String orderBy, String direction, String cursor, Instant after) {
    // 커서가 없다면 첫 페이지 요청이므로 조건 없이 진행
    if (cursor == null && after == null) {
      return null;
    }

    // 1. 좋아요순 정렬일 때
    if ("likeCount".equals(orderBy)) {
      if (cursor == null || after == null) return null; // 파라미터 누락 방어

      long cursorLikeCount = Long.parseLong(cursor); // 메인 커서

      if ("DESC".equalsIgnoreCase(direction)) { // [내림차순]
        return comment.likeCount.lt(cursorLikeCount) // 좋아요 수가 이전 댓글보다 적거나
            .or(comment.likeCount.eq(cursorLikeCount) // 좋아요 수는 같은데
                    .and(comment.createdAt.lt(after)) // 작성일이 더 예전(lt)인 것
            );
      } else { // [오름차순]
        return comment.likeCount.gt(cursorLikeCount) // 좋아요 수가 이전 댓글보다 많거나
            .or(comment.likeCount.eq(cursorLikeCount) // 좋아요 수는 같은데
                    .and(comment.createdAt.gt(after)) // 작성일이 더 최신(gt)인 것
            );
      }
    }

    // 2. 작성일자순 정렬일 때
    if (after == null) return null; // 파라미터 누락 방어

    if ("DESC".equalsIgnoreCase(direction)) { // [내림차순]
      return comment.createdAt.lt(after); // 이전 페이지의 마지막 댓글 작성일자보다 더 과거(lt)인 댓글들만 가져옴
    } else { // [오름차순]
      return comment.createdAt.gt(after); // 이전 페이지의 마지막 댓글 작성일자보다 더 최신(gt)인 댓글들만 가져옴
    }
  }

  // 동적 정렬 조건 생성기
  private OrderSpecifier<?>[] createOrderSpecifier(String orderBy, String direction) {
    Order directionOrder = "ASC".equalsIgnoreCase(direction) ? Order.ASC : Order.DESC;

    if ("likeCount".equals(orderBy)) {
      // 좋아요 정렬 시, 동점일 경우 작성일자 기준으로 한 번 더 정렬
      return new OrderSpecifier[]{
          new OrderSpecifier<>(directionOrder, comment.likeCount), // 1순위: 좋아요 수
          new OrderSpecifier<>(directionOrder, comment.createdAt) // 2순위: 작성일자
      };
    }

    // 최신순 정렬일 때는 조건 하나만 있으면 됨
    return new OrderSpecifier[]{
        new OrderSpecifier<>(directionOrder, comment.createdAt)
    };
  }
}
