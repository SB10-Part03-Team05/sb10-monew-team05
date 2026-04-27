package com.codeit.monew.domain.notification.repository;

import com.codeit.monew.domain.notification.entity.Notification;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  // 알림 전체 읽음 처리용
  @Modifying(clearAutomatically = true)
  @Query("UPDATE Notification n SET n.confirmedAt = CURRENT_TIMESTAMP WHERE n.user.id = :userId AND n.confirmedAt IS NULL")
  int confirmAllByUserId(@Param("userId") UUID userId);

  // 알림 일괄 삭제용
  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM Notification n " +
      "WHERE n.confirmedAt IS NOT NULL " +
      "AND n.createdAt < :targetTime")
  int deleteOldConfirmedNotifications(@Param("targetTime") Instant targetTime);
}
