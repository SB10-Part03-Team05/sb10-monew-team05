package com.codeit.monew.infra.logging;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Profile({"local", "dev"})
@Tag(name = "로그 업로드", description = "S3 로그 업로드 테스트 API")
public class LogUploadController {

  private final LogUploadService logUploadService;
  private final LogUploadProperties logUploadProperties;

  @PostMapping("/upload")
  @Operation(summary = "로그 파일 업로드 수동 실행",
      description = "입력한 조회 기간(days)만큼 과거 날짜를 순회하며 로그를 S3에 업로드합니다.")
  public ResponseEntity<List<LogUploadResult>> upload(
      @Parameter(description = "조회 기간(일 단위), 미입력 시 설정값 사용")
      @RequestParam(required = false) Integer days
  ) {
    // 1. 기준 시간 및 조회 기간(lookbackDays) 설정
    ZoneId zoneId = ZoneId.of(logUploadProperties.getZone());
    LocalDate baseDate = LocalDate.now(zoneId);

    // 파라미터가 있으면 해당 값을 쓰고, 없으면 프로퍼티 설정값을 기본으로 사용
    int lookbackDays =
        (days != null) ? Math.max(1, days) : Math.max(1, logUploadProperties.getLookbackDays());

    log.info("[LOG_UPLOAD_API] manual upload started: lookbackDays={}", lookbackDays);

    List<LogUploadResult> results = new ArrayList<>();

    // 2. 스케줄러와 동일하게 루프를 돌며 업로드 수행
    for (int daysAgo = 1; daysAgo <= lookbackDays; daysAgo++) {
      LocalDate targetDate = baseDate.minusDays(daysAgo);
      try {
        LogUploadResult result = logUploadService.uploadDailyLog(targetDate);
        results.add(result);

        log.info("[LOG_UPLOAD_API] processed: targetDate={}, status={}",
            targetDate, result.status());
      } catch (Exception e) {
        log.error("[LOG_UPLOAD_API] failed: targetDate={}", targetDate, e);
        // 개별 날짜 실패 시 결과 리스트에 에러 상태 추가
        results.add(
            new LogUploadResult(LogUploadResult.Status.UPLOAD_FAILED, targetDate, null, null,
                e.getMessage()));
      }
    }

    // 여러 날짜의 결과를 반환하므로 리스트 전체를 담아 응답 (기본 200 OK)
    return ResponseEntity.ok(results);
  }
}