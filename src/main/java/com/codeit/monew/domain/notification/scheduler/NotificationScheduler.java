package com.codeit.monew.domain.notification.scheduler;

import com.codeit.monew.domain.notification.service.NotificationService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {
  private final NotificationService notificationService;
  private final MeterRegistry meterRegistry;

  // 매일 새벽 0시 0분에 실행 (Cron: 초 분 시 일 월 요일)
  @Scheduled(cron = "0 0 0 * * *")
  public void runNotificationCleanUp() {
    Timer.Sample sample = Timer.start(meterRegistry);
    String status = "success";
    int deletedTotal = 0;
    try {
      log.info("[NOTIFICATION_DELETE] 알림 정리 작업 시작");
      deletedTotal = notificationService.cleanUpOldNotifications();

      // 삭제된 알림의 총 누적 합계를 기록
      if (deletedTotal > 0) {
        meterRegistry.counter("scheduler.notification.deleted.total").increment(deletedTotal);
      }
    } catch (Exception e) {
      status = "fail";
      log.error("[NOTIFICATION_DELETE] 작업 중 예외 발생", e);
      throw e;
    } finally {
      // 작업 횟수 및 결과 기록
      meterRegistry.counter("scheduler.notification.job", "status", status).increment();

      // 작업 소요 시간 기록
      sample.stop(meterRegistry.timer("scheduler.notification.job.time", "status", status));

      log.info("[NOTIFICATION_DELETE] 작업 종료: 상태={}, 삭제건수={}", status, deletedTotal);
    }
  }
}
