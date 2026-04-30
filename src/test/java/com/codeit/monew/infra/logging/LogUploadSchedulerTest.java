package com.codeit.monew.infra.logging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.DateTimeException;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogUploadSchedulerTest {

  @Mock
  private LogUploadService logUploadService;

  private LogUploadProperties logUploadProperties;
  private SimpleMeterRegistry meterRegistry;
  private LogUploadScheduler logUploadScheduler;

  @BeforeEach
  void setUp() {
    logUploadProperties = new LogUploadProperties();
    logUploadProperties.setZone("Asia/Seoul");
    logUploadProperties.setLookbackDays(3); // 기본 3일치 조회 설정

    meterRegistry = new SimpleMeterRegistry();
    logUploadScheduler = new LogUploadScheduler(logUploadProperties, logUploadService,
        meterRegistry);
  }

  @Test
  @DisplayName("업로드 결과가 섞여 있으면 상태별 상세 카운터와 '부분 실패' 작업 카운터를 기록한다")
  void record_detail_counters_and_partial_fail_when_mixed_results() {
    // given: 성공, 스킵, 실패가 각각 1건씩 발생하는 시나리오
    when(logUploadService.uploadDailyLog(any(LocalDate.class)))
        .thenReturn(result(LogUploadResult.Status.UPLOADED_TO_S3))
        .thenReturn(result(LogUploadResult.Status.SKIPPED_FILE_NOT_FOUND))
        .thenReturn(result(LogUploadResult.Status.UPLOAD_FAILED));

    // when: 최근 로그 업로드 스케줄러 실행
    logUploadScheduler.uploadRecentLogs();

    // then: 총 3일치에 대해 서비스가 호출되었는지 확인
    verify(logUploadService, times(3)).uploadDailyLog(any(LocalDate.class));

    // then: 상세 결과(detail)가 각각 카운팅되었는지 검증
    assertCounter("scheduler.log.upload.detail", "result", "UPLOADED", 1.0);
    assertCounter("scheduler.log.upload.detail", "result", "SKIPPED", 1.0);
    assertCounter("scheduler.log.upload.detail", "result", "FAILED", 1.0);

    // then: 결과가 섞여 있으므로 최종 작업 상태는 partial_fail이어야 함
    assertCounter("scheduler.log.upload.job", "status", "partial_fail", 1.0);
    assertTimerCount("scheduler.log.upload.job.time", "status", "partial_fail", 1L);
  }

  @Test
  @DisplayName("일부 날짜 처리 중 예외가 발생해도 다음 날짜를 계속 처리하고 partial_fail로 종료한다")
  void continue_when_single_day_throws_and_finish_partial_fail() {
    // given: 첫 번째 호출은 예외 발생, 두 번째 호출은 성공하는 상황
    when(logUploadService.uploadDailyLog(any(LocalDate.class)))
        .thenThrow(new RuntimeException("single day failed"))
        .thenReturn(result(LogUploadResult.Status.UPLOADED_TO_S3));

    logUploadProperties.setLookbackDays(2);

    // when: 스케줄러 실행
    logUploadScheduler.uploadRecentLogs();

    // then: 예외가 발생해도 중단되지 않고 2번의 호출이 모두 완료되어야 함
    verify(logUploadService, times(2)).uploadDailyLog(any(LocalDate.class));

    // then: 예외 건은 FAILED로, 성공 건은 UPLOADED로 집계
    assertCounter("scheduler.log.upload.detail", "result", "FAILED", 1.0);
    assertCounter("scheduler.log.upload.detail", "result", "UPLOADED", 1.0);
    assertCounter("scheduler.log.upload.job", "status", "partial_fail", 1.0);
  }

  @Test
  @DisplayName("조회 기간(lookbackDays)이 0 이하이면 최소 1일(어제) 데이터는 처리한다")
  void process_at_least_one_day_when_lookback_days_is_non_positive() {
    // given: 설정을 0으로 지정
    logUploadProperties.setLookbackDays(0);
    when(logUploadService.uploadDailyLog(any(LocalDate.class)))
        .thenReturn(result(LogUploadResult.Status.SKIPPED_ALREADY_EXISTS));

    // when: 스케줄러 실행
    logUploadScheduler.uploadRecentLogs();

    // then: 최소 1회는 실행되어야 함 (방어 로직)
    verify(logUploadService, times(1)).uploadDailyLog(any(LocalDate.class));
    assertCounter("scheduler.log.upload.job", "status", "success", 1.0);
  }

  @Test
  @DisplayName("시간대(zone) 설정이 잘못되면 작업을 실행하지 않고 fail 상태를 기록한 뒤 예외를 전파한다")
  void fail_and_rethrow_when_zone_is_invalid() {
    // given: 잘못된 타임존 설정
    logUploadProperties.setZone("Invalid/Zone");

    // when & then: 예외가 상위로 던져지는지 확인
    assertThrows(DateTimeException.class, () -> logUploadScheduler.uploadRecentLogs());

    // then: 서비스 호출은 수행되지 않아야 함
    verify(logUploadService, never()).uploadDailyLog(any(LocalDate.class));

    // then: 작업 지표에는 실패(fail) 상태가 기록되어야 함
    assertCounter("scheduler.log.upload.job", "status", "fail", 1.0);
    assertTimerCount("scheduler.log.upload.job.time", "status", "fail", 1L);
  }

  private LogUploadResult result(LogUploadResult.Status status) {
    return new LogUploadResult(status, LocalDate.of(2026, 4, 29), "source", "s3", "ok");
  }

  private void assertCounter(String name, String tagKey, String tagValue, double expected) {
    double actual = meterRegistry.counter(name, tagKey, tagValue).count();
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
  }

  private void assertTimerCount(String name, String tagKey, String tagValue, long expected) {
    long actual = meterRegistry.timer(name, tagKey, tagValue).count();
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
  }
}
