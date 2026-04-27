package com.codeit.monew.domain.notification.scheduler;

import com.codeit.monew.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {
  private final NotificationService notificationService;

  // 매일 새벽 0시 0분에 실행 (Cron: 초 분 시 일 월 요일)
  @Scheduled(cron = "0 0 0 * * *")
  public void runNotificationCleanUp() {
    notificationService.cleanUpOldNotifications();
  }
}
