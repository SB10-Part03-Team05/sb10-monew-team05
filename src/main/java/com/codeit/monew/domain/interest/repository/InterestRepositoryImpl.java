package com.codeit.monew.domain.interest.repository;

import com.codeit.monew.domain.interest.dto.response.CursorPageResponseInterestDto;
import com.codeit.monew.domain.interest.dto.response.InterestDto;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.entity.QInterest;
import com.codeit.monew.domain.interest.entity.QKeyword;
import com.codeit.monew.domain.interest.entity.QSubscription;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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
      String direction, String cursor, int limit, UUID userId) {
    // 1. 검색 + 커서 조건으로 Interest 엔티티 조회
    List<Interest> interests = queryFactory
        .selectFrom(interest)
        .leftJoin(interest.keywords, keyword)
        .where(
            buildSearchCondition(searchKeyword),
            buildCursorCondition(orderBy, direction, cursor)
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
        .distinct()
        .leftJoin(interest.keywords, keyword).fetchJoin()
        .where(interest.id.in(interestIds))
        .fetch();

    // 4. 원본 정렬 순서 유지
    Map<UUID, Interest> interestMap = interestsWithKeywords.stream()
        .collect(Collectors.toMap(Interest::getId, Function.identity(), (a, b) -> a));
    List<Interest> orderedInterests = interestIds.stream()
        .map(interestMap::get)
        .toList();

    // 5. 구독 여부 조회
    Set<UUID> subscribedIds = new HashSet<>(queryFactory
        .select(subscription.interest.id)
        .from(subscription)
        .where(
            subscription.user.id.eq(userId),
            subscription.interest.id.in(interestIds)
        )
        .fetch());

    // 6. InterestDto 변환
    List<InterestDto> content = orderedInterests.stream()
        .map(i -> InterestDto.from(i, subscribedIds.contains(i.getId())))
        .toList();

    // 7. nextCursor 계산
    String nextCursor = null;
    if (hasNext && !interests.isEmpty()) {
      Interest last = interests.get(interests.size() - 1);
      String raw = "name".equals(orderBy)
          ? last.getName() + "::" + last.getId()
          : last.getSubscriberCount() + "::" + last.getId();
      nextCursor = Base64.getEncoder().encodeToString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    // 8. totalElements
    Long totalElements = queryFactory
        .select(interest.countDistinct())
        .from(interest)
        .leftJoin(interest.keywords, keyword)
        .where(buildSearchCondition(searchKeyword))
        .fetchOne();

    return new CursorPageResponseInterestDto(
        content,
        nextCursor,
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
   * cursor는 Base64로 인코딩된 'value::uuid' 형식이며 uuid로 tie-breaking 처리
   * cursor가 없으면 null 반환 (첫 페이지)
   */
  private BooleanExpression buildCursorCondition(String orderBy, String direction, String cursor) {
    validateSortArgs(orderBy, direction);
    if (cursor == null)
      return null;

    // cursor 파싱
    String decoded;
    try {
      decoded = new String(Base64.getDecoder().decode(cursor), java.nio.charset.StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
       throw new IllegalArgumentException("잘못된 cursor 인코딩입니다: " + cursor);
    }
    String[] parts = decoded.split("::", 2);
    if (parts.length != 2) {
      throw new IllegalArgumentException("잘못된 cursor 형식입니다. cursor는 'value::uuid' 형식이어야 합니다: " + cursor);
    }
    String cursorValue = parts[0];
    UUID cursorId;
    try {
      cursorId = UUID.fromString(parts[1]);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("잘못된 cursor ID 형식입니다: " + parts[1]);
    }

    boolean isAsc = "ASC".equalsIgnoreCase(direction);

    if ("name".equals(orderBy)) {
      return isAsc
          ? interest.name.gt(cursorValue)
          .or(interest.name.eq(cursorValue)
              .and(Expressions.stringTemplate("cast({0} as text)", interest.id)
                  .gt(cursorId.toString())))
          : interest.name.lt(cursorValue)
              .or(interest.name.eq(cursorValue)
                  .and(Expressions.stringTemplate("cast({0} as text)", interest.id)
                      .lt(cursorId.toString())));
    } else {
      long cursorLong;
      try {
        cursorLong = Long.parseLong(cursorValue);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("subscriberCount 정렬 시 cursor는 숫자여야 합니다: " + cursorValue);
      }
      return isAsc
          ? interest.subscriberCount.gt(cursorLong)
          .or(interest.subscriberCount.eq(cursorLong)
              .and(Expressions.stringTemplate("cast({0} as text)", interest.id)
                  .gt(cursorId.toString())))
          : interest.subscriberCount.lt(cursorLong)
              .or(interest.subscriberCount.eq(cursorLong)
                  .and(Expressions.stringTemplate("cast({0} as text)", interest.id)
                      .lt(cursorId.toString())));
    }
  }

  /**
   * 정렬 조건 생성
   * 정렬 기준(name/subscriberCount)과 방향(ASC/DESC)에 따라 정렬 조건 생성
   * 동일값 존재 시 createdAt을 보조 정렬 기준으로 사용
   */
  private OrderSpecifier<?>[] buildOrderSpecifiers(String orderBy, String direction) {
    validateSortArgs(orderBy, direction);
    boolean isAsc = "ASC".equalsIgnoreCase(direction);
    if ("name".equals(orderBy)) {
      return new OrderSpecifier[]{
          isAsc ? interest.name.asc() : interest.name.desc(),
          isAsc ? interest.id.asc() : interest.id.desc()
      };
    } else {
      return new OrderSpecifier[]{
          isAsc ? interest.subscriberCount.asc() : interest.subscriberCount.desc(),
          isAsc ? interest.id.asc() : interest.id.desc()
      };
    }
  }

  private void validateSortArgs(String orderBy, String direction) {
    boolean validOrderBy = "name".equals(orderBy) || "subscriberCount".equals(orderBy);
    boolean validDirection = "ASC".equalsIgnoreCase(direction) || "DESC".equalsIgnoreCase(direction);
    if (!validOrderBy || !validDirection) {
      throw new IllegalArgumentException("지원하지 않는 정렬 파라미터입니다. orderBy=name|subscriberCount, direction=ASC|DESC");
      }
    }
}
