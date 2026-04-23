package com.codeit.monew.infra.logging;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Profile({"local", "dev"})
@Tag(name = "로그 업로드", description = "S3 로그 업로드 테스트 API")
public class LogUploadController {

  private final LogUploadService logUploadService;
  private final LogUploadProperties logUploadProperties;

  @PostMapping("/upload")
  @Operation(summary = "로그 파일 업로드 수동 실행", description = "스케줄러 대신 특정 날짜 로그를 S3에 수동 업로드합니다.")
  public ResponseEntity<LogUploadResult> upload(
      @Parameter(description = "업로드 대상 날짜(yyyy-MM-dd), 미입력 시 전일")
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
  ) {
    ZoneId zoneId = ZoneId.of(logUploadProperties.getZone());
    LocalDate date = targetDate != null ? targetDate : LocalDate.now(zoneId).minusDays(1);

    LogUploadResult result = logUploadService.uploadDailyLog(date);

    HttpStatus status = switch (result.status()) {
      case UPLOADED_TO_S3 -> HttpStatus.OK;
      case SKIPPED_FILE_NOT_FOUND -> HttpStatus.NOT_FOUND;
      case UPLOAD_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
    };

    return ResponseEntity.status(status).body(result);
  }
}
