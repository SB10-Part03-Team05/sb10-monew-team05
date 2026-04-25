package com.codeit.monew.domain.notification.repository.impl;

import com.codeit.monew.domain.notification.entity.Notification;
import com.codeit.monew.domain.notification.entity.QNotification;
import com.codeit.monew.domain.notification.repository.NotificationQueryRepository;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationQueryRepositoryImpl implements NotificationQueryRepository {
  private final JPAQueryFactory queryFactory;

  @Override
  public List<Notification> findUnconfirmedByCursor(UUID userId, Instant after, UUID cursor, int limit) {
    QNotification notification = QNotification.notification;

    return queryFactory
        .selectFrom(notification)
        .where(
            notification.user.id.eq(userId), // 해당 유저의 알림
            notification.confirmed.isFalse(), // 미확인 알림
            ltCursor(after, cursor) // 커서 페이징 조건 (이전 페이지의 마지막 데이터 다음부터)
        )
        .orderBy(notification.createdAt.desc(), notification.id.desc()) // 최신 알람부터 보이도록 정렬
        .limit(limit + 1) // 다음 페이지 존재 여부 확인용
        .fetch();
  }

  @Override
  public long countUnconfirmedByUserId(UUID userId) {
    QNotification notification = QNotification.notification;

    Long count = queryFactory
        .select(notification.count()) // 미확인 알림 개수
        .from(notification) // 해당 유저의 알림
        .where(notification.user.id.eq(userId), notification.confirmed.isFalse()) // 미확인 알림
        .fetchOne();

    return count != null ? count : 0L;
  }

  // 복합 커서 로직
  private BooleanExpression ltCursor(Instant after, UUID cursor) {
    // 첫 페이지 조회 시에는 조건문 무시
    if (after == null || cursor == null) return null;

    QNotification notification = QNotification.notification;

    // 정렬 조건
    return notification.createdAt.lt(after) // 1순위: 생성 시간이 더 과거인 것
        .or(notification.createdAt.eq(after).and(notification.id.lt(cursor))); // 2순위: 고유 ID값이 더 작은 것
  }
}
