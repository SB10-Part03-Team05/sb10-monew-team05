package com.codeit.monew.infra.logging;

import java.time.LocalDate;

public record LogUploadResult(
    Status status,
    LocalDate targetDate,
    String sourcePath,
    String s3Key,
    String message
) {

  public enum Status {
    UPLOADED_TO_S3,
    SKIPPED_FILE_NOT_FOUND,
    SKIPPED_ALREADY_EXISTS,
    UPLOAD_FAILED
  }
}
