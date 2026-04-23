package com.codeit.monew.infra.logging;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 로그 파일을 클라우드 저장소(S3 등)로 정기 업로드하는 스케줄러입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogUploadScheduler {

  private final LogUploadProperties logUploadProperties;
  private final LogUploadService logUploadService;

  @Scheduled(
      cron = "${monew.logging.upload.cron}",
      zone = "${monew.logging.upload.zone}"
  )
  public void uploadYesterdayLog() {
    // 1. 업로드 대상 날짜 계산 (어제 날짜)
    ZoneId zoneId = ZoneId.of(logUploadProperties.getZone());
    LocalDate targetDate = LocalDate.now(zoneId).minusDays(1);

    log.info("[LOG_UPLOAD] scheduled upload started: targetDate={}", targetDate);

    try {
      // 2. 서비스 계층을 통해 로그 업로드 실행
      LogUploadResult result = logUploadService.uploadDailyLog(targetDate);

      // 3. 결과 로그 기록
      log.info("[LOG_UPLOAD] scheduled upload finished: status={}, message={}",
          result.status(),
          result.message()
      );
    } catch (Exception e) {
      // 예기치 못한 에러 발생 시 로그 기록
      log.error("[LOG_UPLOAD] scheduled upload failed: targetDate={}, error={}",
          targetDate,
          e.getMessage()
      );
    }
  }
}