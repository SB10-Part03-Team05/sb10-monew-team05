package com.codeit.monew.infra.logging;

import com.codeit.monew.global.config.AwsProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogUploadService {

  private final S3Client s3Client;
  private final AwsProperties awsProperties;
  private final LogUploadProperties logUploadProperties;

  public LogUploadResult uploadDailyLog(LocalDate targetDate) {
    // 1. 업로드할 로컬 파일 경로 결정
    Path sourceLogFile = resolveLogFile(targetDate);

    // 2. 파일 존재 여부 확인 (없으면 스킵)
    if (Files.notExists(sourceLogFile)) {
      return skipUpload(targetDate, sourceLogFile);
    }

    // 3. S3 설정 확인 (버킷 이름 필수)
    if (!StringUtils.hasText(awsProperties.getBucket())) {
      return failUpload(targetDate, sourceLogFile);
    }

    // 4. 업로드 실행 (재시도 로직 포함)
    return executeUploadWithRetry(targetDate, sourceLogFile);
  }

  /**
   * 재시도 메커니즘을 포함한 실제 업로드 처리
   */
  private LogUploadResult executeUploadWithRetry(LocalDate targetDate, Path sourceLogFile) {
    String objectKey = buildObjectKey(targetDate, sourceLogFile.getFileName().toString());
    int maxRetries = Math.max(1, logUploadProperties.getMaxRetries());
    Exception lastException = null;

    for (int attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        // S3에 실제 파일 전송
        putObject(sourceLogFile, objectKey);

        log.info("[LOG_UPLOAD] 업로드 성공: key={}, 시도 횟수={}", objectKey, attempt);
        return new LogUploadResult(LogUploadResult.Status.UPLOADED_TO_S3, targetDate,
            sourceLogFile.toString(), objectKey, "성공");
      } catch (Exception e) {
        lastException = e;
        handleRetry(attempt, maxRetries, objectKey, e);
      }
    }

    log.error("[LOG_UPLOAD] 모든 재시도 실패: key={}", objectKey, lastException);
    return new LogUploadResult(LogUploadResult.Status.UPLOAD_FAILED, targetDate,
        sourceLogFile.toString(), objectKey, "재시도 초과 실패");
  }

  /**
   * S3 PutObject 요청 수행
   */
  private void putObject(Path sourceLogFile, String objectKey) {
    PutObjectRequest request = PutObjectRequest.builder()
        .bucket(awsProperties.getBucket())
        .key(objectKey)
        .build();

    s3Client.putObject(request, sourceLogFile);
  }

  /**
   * 재시도 간 대기 및 로그 기록
   */
  private void handleRetry(int attempt, int maxRetries, String key, Exception e) {
    if (attempt < maxRetries) {
      log.warn("[LOG_UPLOAD] 업로드 실패, 재시도 중: {}/{}회, key={}, 이유={}", attempt, maxRetries, key,
          e.getMessage());
      try {
        Thread.sleep(Math.max(0L, logUploadProperties.getRetryDelayMs()));
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * 로컬 로그 파일 경로 생성 (예: ./application.2026-04-23.log)
   */
  private Path resolveLogFile(LocalDate targetDate) {
    String fileName = logUploadProperties.getFilePrefix() + "." + targetDate + ".log";
    return Paths.get(logUploadProperties.getLogDir(), fileName);
  }

  /**
   * S3 저장 경로(Key) 생성 (예: logs/application.2026-04-23.log)
   */
  private String buildObjectKey(LocalDate targetDate, String fileName) {
    String prefix = trimSlash(logUploadProperties.getS3Prefix());
    return StringUtils.hasText(prefix) ? prefix + "/" + fileName : fileName;
  }

  private String trimSlash(String value) {
    return (value == null) ? "" : value.replaceAll("^/+|/+$", "");
  }

  // 결과 반환 헬퍼 메서드들
  private LogUploadResult skipUpload(LocalDate date, Path path) {
    log.warn("[LOG_UPLOAD] 작업 건너뜀: {} - {}", path, "로그 파일을 찾을 수 없습니다.");
    return new LogUploadResult(LogUploadResult.Status.SKIPPED_FILE_NOT_FOUND, date, path.toString(),
        null, "로그 파일을 찾을 수 없습니다.");
  }

  private LogUploadResult failUpload(LocalDate date, Path path) {
    log.error("[LOG_UPLOAD] 업로드 실패: {}", "S3 버킷 설정이 비어있습니다.");
    return new LogUploadResult(LogUploadResult.Status.UPLOAD_FAILED, date, path.toString(), null,
        "S3 버킷 설정이 비어있습니다.");
  }
}
