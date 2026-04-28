package com.codeit.monew.infra.logging;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 로그 파일을 클라우드 저장소(S3 등)로 정기 업로드하는 스케줄러입니다. 설정된 lookbackDays만큼 과거 날짜를 순회하며 누락된 로그가 없는지 체크하고 업로드합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogUploadScheduler {

  private final LogUploadProperties logUploadProperties;
  private final LogUploadService logUploadService;
  private final MeterRegistry meterRegistry;

  /**
   * 지정된 스케줄(Cron)에 따라 로그 업로드 작업을 수행합니다.
   */
  @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
  public void uploadRecentLogs() {

    Timer.Sample sample = Timer.start(meterRegistry);
    String jobStatus = "success";

    // 1. 기준 시간 및 조회 기간 설정
    ZoneId zoneId = ZoneId.of(logUploadProperties.getZone());
    LocalDate baseDate = LocalDate.now(zoneId);

    // 최소 1일은 조회하도록 설정
    int lookbackDays = Math.max(1, logUploadProperties.getLookbackDays());

    log.info("[LOG_UPLOAD] scheduled upload started: lookbackDays={}", lookbackDays);

    // 결과 통계를 위한 카운터 초기화
    int uploadedCount = 0;
    int skippedCount = 0;
    int failedCount = 0;

    try {
      // 2. 1일 전부터 lookbackDays 전까지 역순으로 루프를 돌며 업로드 시도
      for (int daysAgo = 1; daysAgo <= lookbackDays; daysAgo++) {
        LocalDate targetDate = baseDate.minusDays(daysAgo);

        try {
          // 서비스 계층을 통해 개별 날짜 로그 업로드 실행
          LogUploadResult result = logUploadService.uploadDailyLog(targetDate);

          // 결과 상태에 따른 통계 처리
          switch (result.status()) {
            case UPLOADED_TO_S3 -> {
              uploadedCount++;
              recordDetail("UPLOADED");
            }
            case SKIPPED_FILE_NOT_FOUND, SKIPPED_ALREADY_EXISTS -> {
              skippedCount++;
              recordDetail("SKIPPED");
            }
            case UPLOAD_FAILED -> {
              failedCount++;
              recordDetail("FAILED");
            }
          }

          log.info("[LOG_UPLOAD] scheduled upload processed: targetDate={}, status={}, message={}",
              targetDate, result.status(), result.message());

        } catch (Exception e) {
          // 개별 날짜 처리 중 예외 발생 시, 전체 루프를 멈추지 않고 에러 로그 기록 후 다음 날짜로 진행
          failedCount++;
          jobStatus = "partial_fail";
          recordDetail("FAILED");
          log.error("[LOG_UPLOAD] scheduled upload failed: targetDate={}, error={}",
              targetDate, e.getMessage(), e);
        }
      }
    } catch (Exception e) {
      jobStatus = "fail"; // for 루프 자체가 붕괴되는 심각한 에러 발생 시
      log.error("[LOG_UPLOAD] 스케줄러 전체 로직 에러", e);
      throw e;
    } finally {
      // 작업 횟수 및 결과 기록
      meterRegistry.counter("scheduler.log.upload.job", "status", jobStatus).increment();

      // 작업 소요 시간 기록
      sample.stop(meterRegistry.timer("scheduler.log.upload.job.time", "status", jobStatus));

      // 3. 전체 작업 결과 요약 출력
      log.info("[LOG_UPLOAD] scheduled upload summary: uploaded={}, skipped={}, failed={}",
          uploadedCount, skippedCount, failedCount);
    }
  }

  // 세부 상태별 건수를 메트릭에 포함시기는 헬퍼 메서드
  private void recordDetail(String resultStatus) {
    meterRegistry.counter("scheduler.log.upload.detail", "result", resultStatus).increment();
  }
}