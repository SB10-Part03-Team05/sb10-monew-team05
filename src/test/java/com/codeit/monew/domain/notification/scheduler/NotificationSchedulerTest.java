package com.codeit.monew.domain.notification.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.notification.service.NotificationService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {
  @Mock
  private NotificationService notificationService;

  private SimpleMeterRegistry meterRegistry;

  private NotificationScheduler notificationScheduler;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    notificationScheduler = new NotificationScheduler(notificationService, meterRegistry);
  }

  @Test
  @DisplayName("알림 정리가 성공하면 메트릭에 성공 상태와 삭제 건수가 기록된다.")
  void runNotificationCleanUp_success() {
    // given
    int deletedCount = 5;
    given(notificationService.cleanUpOldNotifications()).willReturn(deletedCount);

    // when
    notificationScheduler.runNotificationCleanUp();

    // then
    verify(notificationService, times(1)).cleanUpOldNotifications();
    assertThat(meterRegistry.counter("scheduler.notification.deleted.total").count())
        .isEqualTo(deletedCount); // 총 삭제 건수가 5만큼 증가했는지 검증
    assertThat(meterRegistry.counter("scheduler.notification.job", "status", "success").count())
        .isEqualTo(1); // 작업 상태가 success로 기록되었는지 검증
    assertThat(meterRegistry.timer("scheduler.notification.job.time", "status", "success").count())
        .isEqualTo(1L); // 타이머가 success 상태로 측정되었는지 검증
  }

  @Test
  @DisplayName("삭제할 알림이 없을 경우 삭제 건수 메트릭은 증가하지 않는다.")
  void runNotificationCleanUp_success_no_deletion() {
    // given
    given(notificationService.cleanUpOldNotifications()).willReturn(0);

    // when
    notificationScheduler.runNotificationCleanUp();

    // then
    assertThat(meterRegistry.find("scheduler.notification.deleted.total").counter()).isNull(); // 삭제 카운터가 생성 및 증가되지 않아야 함
    assertThat(meterRegistry.counter("scheduler.notification.job", "status", "success").count())
        .isEqualTo(1); // 작업 성공 여부 카운터는 정상적으로 올라가야 함
  }

  @Test
  @DisplayName("작업 중 예외가 발생하면 fail 상태가 기록되고 예외가 던져진다.")
  void runNotificationCleanUp_fail() {
    // given
    RuntimeException exception = new RuntimeException("DB Timeout");
    given(notificationService.cleanUpOldNotifications()).willThrow(exception);

    // when & then
    assertThatThrownBy(() -> notificationScheduler.runNotificationCleanUp())
        .isInstanceOf(RuntimeException.class)
        .hasMessage("DB Timeout");

    assertThat(meterRegistry.counter("scheduler.notification.job", "status", "fail").count())
        .isEqualTo(1); // 작업 상태가 fail로 기록되었는지 검증
    assertThat(meterRegistry.timer("scheduler.notification.job.time", "status", "fail").count())
        .isEqualTo(1L); // 타이머가 fail 상태로 완료되었는지 검증
  }
}