package com.codeit.monew.domain.notification.repository;

import com.codeit.monew.domain.notification.entity.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationQueryRepository {

  // 동적 정렬 및 커서 페이징을 적용한 알림 목록 조회
  List<Notification> findUnconfirmedByCursor(UUID userId, Instant after, UUID cursor, int limit);

  // 특정 유저의 미확인 알림 개수 조회
  long countUnconfirmedByUserId(UUID userId);
}
