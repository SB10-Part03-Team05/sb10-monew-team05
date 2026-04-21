package com.codeit.monew.domain.interest.repository;

import com.codeit.monew.domain.interest.dto.response.CursorPageResponseInterestDto;
import com.codeit.monew.domain.interest.dto.response.InterestDto;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.entity.QInterest;
import com.codeit.monew.domain.interest.entity.QKeyword;
import com.codeit.monew.domain.interest.entity.QSubscription;
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
public class InterestRepositoryImpl implements InterestRepositoryCustom{

  private final JPAQueryFactory queryFactory;

  private static final QInterest interest = QInterest.interest;
  private static final QKeyword keyword = QKeyword.keyword;
  private static final QSubscription subscription = QSubscription.subscription;

  @Override
  public CursorPageResponseInterestDto findInterests(String searchKeyword, String orderBy,
      String direction, String cursor, Instant after, int limit, UUID userId) {
    // 1. 검색 + 커서 조건으로 Interest 엔티티 조회
    List<Interest> interests = queryFactory
        .selectFrom(interest)
        .leftJoin(interest.keywords, keyword)
        .where(
            buildSearchCondition(searchKeyword),
            buildCursorCondition(orderBy, direction, cursor, after)
        )
        .groupBy(interest.id)
        .orderBy(buildOrderSpecifiers(orderBy, direction))
        .limit(limit + 1)
        .fetch();

    // 2. hasNext 판단
    boolean hasNext = interests.size() > limit;
    if (hasNext) {
      interests = interests.subList(0, limit);
    }

    // 3. 키워드 fetch (N+1 방지)
    List<UUID> interestIds = interests.stream()
        .map(Interest::getId)
        .toList();

    List<Interest> interestsWithKeywords = queryFactory
        .selectFrom(interest)
        .leftJoin(interest.keywords, keyword).fetchJoin()
        .where(interest.id.in(interestIds))
        .fetch();

    // 4. 구독 여부 조회
    List<UUID> subscribedIds = queryFactory
        .select(subscription.interest.id)
        .from(subscription)
        .where(subscription.user.id.eq(userId))
        .fetch();

    // 5. InterestDto 변환
    List<InterestDto> content = interestsWithKeywords.stream()
        .map(i -> InterestDto.from(i, subscribedIds.contains(i.getId())))
        .toList();

    // 6. nextCursor, nextAfter 계산
    String nextCursor = null;
    Instant nextAfter = null;
    if (hasNext && !interests.isEmpty()) {
      Interest last = interests.get(interests.size() - 1);
      nextCursor = "name".equals(orderBy)
          ? last.getName()
          : String.valueOf(last.getSubscriberCount());
      nextAfter = last.getCreatedAt();
    }

    // 7. totalElements
    Long totalElements = queryFactory
        .select(interest.countDistinct())
        .from(interest)
        .leftJoin(interest.keywords, keyword)
        .where(buildSearchCondition(searchKeyword))
        .fetchOne();

    return new CursorPageResponseInterestDto(
        content,
        nextCursor,
        nextAfter,
        content.size(),
        totalElements != null ? totalElements : 0L,
        hasNext
    );
  }

  /**
   * 검색어 기반 부분일치 조건 생성
   * 관심사 이름 또는 키워드 이름에 검색어가 포함되면 조회
   * 검색어가 없으면 null 반환
   */
  private BooleanExpression buildSearchCondition(String searchKeyword) {
    if (searchKeyword == null || searchKeyword.isBlank()) return null;
    return interest.name.containsIgnoreCase(searchKeyword)
        .or(keyword.name.containsIgnoreCase(searchKeyword));
  }

  /**
   * 커서 기반 페이지네이션 조건 생성
   * 정렬 기준(name/subscriberCount)과 방향(ASC/DESC)에 따라 커서 조건 생성
   * 동일값 존재 시 createdAt으로 tie-breaking 처리
   * cursor 또는 after가 없으면 null 반환 (첫 페이지)
   */
  private BooleanExpression buildCursorCondition(String orderBy, String direction, String cursor, Instant after) {
    if (cursor == null || after == null) return null;
    boolean isAsc = "ASC".equalsIgnoreCase(direction);

    if ("name".equals(orderBy)) {
      return isAsc
          ? interest.name.gt(cursor)
          .or(interest.name.eq(cursor).and(interest.createdAt.gt(after)))
          : interest.name.lt(cursor)
              .or(interest.name.eq(cursor).and(interest.createdAt.lt(after)));
    } else {
      long cursorValue = Long.parseLong(cursor);
      return isAsc
          ? interest.subscriberCount.gt(cursorValue)
          .or(interest.subscriberCount.eq(cursorValue).and(interest.createdAt.gt(after)))
          : interest.subscriberCount.lt(cursorValue)
              .or(interest.subscriberCount.eq(cursorValue).and(interest.createdAt.lt(after)));
    }
  }

  /**
   * 정렬 조건 생성
   * 정렬 기준(name/subscriberCount)과 방향(ASC/DESC)에 따라 정렬 조건 생성
   * 동일값 존재 시 createdAt을 보조 정렬 기준으로 사용
   */
  private OrderSpecifier<?>[] buildOrderSpecifiers(String orderBy, String direction) {
    boolean isAsc = "ASC".equalsIgnoreCase(direction);
    if ("name".equals(orderBy)) {
      return new OrderSpecifier[]{
          isAsc ? interest.name.asc() : interest.name.desc(),
          isAsc ? interest.createdAt.asc() : interest.createdAt.desc()
      };
    } else {
      return new OrderSpecifier[]{
          isAsc ? interest.subscriberCount.asc() : interest.subscriberCount.desc(),
          isAsc ? interest.createdAt.asc() : interest.createdAt.desc()
      };
    }
  }
}
