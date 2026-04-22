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

    // 메인 커서 기준값과 UUID 문자열 분리
    String realCursor = request.cursor(); // 메인 커서 값
    UUID cursorId = null;                 // 고유 ID 커서 값

    if (request.cursor() != null && request.cursor().contains("_")) {
      String[] parts = request.cursor().split("_");
      realCursor = parts[0];
      cursorId = UUID.fromString(parts[1]); // 문자열을 다시 UUID 객체로 복원
    }

    return queryFactory
        .selectFrom(comment)
        .leftJoin(comment.user).fetchJoin() // 사용자(닉네임) 정보를 한 번의 쿼리로 다 끌고 옴 (N+1 방어)
        .where(
            comment.article.id.eq(articleId), // 해당 기사의 댓글만 필터링
            comment.deletedAt.isNull(), // 논리 삭제된 댓글 제외
            cursorCondition(request.orderBy(), request.direction(), realCursor, request.after(), cursorId)
        )
        .orderBy(createOrderSpecifier(request.orderBy(), request.direction()))
        .limit(request.limit() + 1) // hasNext 판별을 위해 요청한 개수보다 1개 더 가져옴
        .fetch();
  }

  // 동적 커서 조건 생성기
  private BooleanExpression cursorCondition(String orderBy, String direction, String cursor, Instant after, UUID cursorId) {
    // 커서가 없다면 첫 페이지 요청이므로 조건 없이 진행
    if (cursor == null && after == null) {
      return null;
    }

    // 1. 좋아요순 정렬일 때
    if ("likeCount".equals(orderBy)) {
      if (cursor == null || after == null || cursorId == null) return null; // 파라미터 누락 방어

      long cursorLikeCount = Long.parseLong(cursor); // 메인 커서

      if ("DESC".equalsIgnoreCase(direction)) { // [내림차순]
        return comment.likeCount.lt(cursorLikeCount) // 좋아요 수가 이전 댓글보다 적거나
            .or(comment.likeCount.eq(cursorLikeCount) // 좋아요 수는 같은데
                .and(comment.createdAt.lt(after)) // 작성일이 더 예전(lt)인 것
                .or(comment.createdAt.eq(after).and(comment.id.lt(cursorId))) // 고유 ID로 최종 동점자 판별
            );
      } else { // [오름차순]
        return comment.likeCount.gt(cursorLikeCount) // 좋아요 수가 이전 댓글보다 많거나
            .or(comment.likeCount.eq(cursorLikeCount) // 좋아요 수는 같은데
                .and(comment.createdAt.gt(after)) // 작성일이 더 최신(gt)인 것
                .or(comment.createdAt.eq(after).and(comment.id.gt(cursorId))) // 고유 ID로 최종 동점자 판별
            );
      }
    }

    // 2. 작성일자순 정렬일 때
    if (after == null || cursorId == null) return null; // 파라미터 누락 방어

    if ("DESC".equalsIgnoreCase(direction)) { // [내림차순]
      return comment.createdAt.lt(after) // 이전 페이지의 마지막 댓글 작성일자보다 더 과거(lt)인 댓글들만 가져옴
          .or(comment.createdAt.eq(after).and(comment.id.lt(cursorId))); // 고유 ID로 최종 동점자 판별
    } else { // [오름차순]
      return comment.createdAt.gt(after) // 이전 페이지의 마지막 댓글 작성일자보다 더 최신(gt)인 댓글들만 가져옴
          .or(comment.createdAt.eq(after).and(comment.id.gt(cursorId))); // 고유 ID로 최종 동점자 판별
    }
  }

  // 동적 정렬 조건 생성기
  private OrderSpecifier<?>[] createOrderSpecifier(String orderBy, String direction) {
    Order directionOrder = "ASC".equalsIgnoreCase(direction) ? Order.ASC : Order.DESC;

    if ("likeCount".equals(orderBy)) {
      // 좋아요 정렬 시
      return new OrderSpecifier[]{
          new OrderSpecifier<>(directionOrder, comment.likeCount), // 1순위: 좋아요 수
          new OrderSpecifier<>(directionOrder, comment.createdAt), // 2순위: 작성일자
          new OrderSpecifier<>(directionOrder, comment.id) // 3순위: 고유 ID
      };
    }

    // 최신순 정렬 시
    return new OrderSpecifier[]{
        new OrderSpecifier<>(directionOrder, comment.createdAt), // 1순위: 작성일자
        new OrderSpecifier<>(directionOrder, comment.id) // 2순위: 고유 ID
    };
  }
}
