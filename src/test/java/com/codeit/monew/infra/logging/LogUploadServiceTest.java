package com.codeit.monew.infra.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

import com.codeit.monew.global.config.AwsProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@ExtendWith(MockitoExtension.class)
class LogUploadServiceTest {

  @Mock
  private S3Client s3Client;

  private AwsProperties awsProperties;
  private LogUploadProperties logUploadProperties;
  private LogUploadService logUploadService;

  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() {
    awsProperties = new AwsProperties();
    awsProperties.setBucket("test-log-bucket");

    logUploadProperties = new LogUploadProperties();
    logUploadProperties.setLogDir(tempDir.toString());
    logUploadProperties.setFilePrefix("application");
    logUploadProperties.setS3Prefix("/logs/");
    logUploadProperties.setMaxRetries(3);
    logUploadProperties.setRetryDelayMs(0);

    logUploadService = new LogUploadService(s3Client, awsProperties, logUploadProperties);
  }

  @Test
  @DisplayName("로그 파일이 없으면 업로드를 건너뛴다")
  void should_skip_upload_when_log_file_not_found() {
    // given
    LocalDate targetDate = LocalDate.of(2026, 4, 22);

    // when
    LogUploadResult result = logUploadService.uploadDailyLog(targetDate);

    // then
    assertThat(result.status()).isEqualTo(LogUploadResult.Status.SKIPPED_FILE_NOT_FOUND);
    assertThat(result.s3Key()).isNull();
    verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(Path.class));
  }

  @Test
  @DisplayName("S3 버킷 설정이 비어 있으면 업로드에 실패한다")
  void should_fail_upload_when_bucket_is_empty() throws IOException {
    // given
    LocalDate targetDate = LocalDate.of(2026, 4, 22);
    Files.createFile(tempDir.resolve("application.2026-04-22.log"));
    awsProperties.setBucket(" ");

    // when
    LogUploadResult result = logUploadService.uploadDailyLog(targetDate);

    // then
    assertThat(result.status()).isEqualTo(LogUploadResult.Status.UPLOAD_FAILED);
    assertThat(result.s3Key()).isNull();
    verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(Path.class));
  }

  @Test
  @DisplayName("업로드에 성공하면 S3 객체 키를 포함한 결과를 반환한다")
  void should_upload_log_to_s3_successfully() throws IOException {
    // given
    LocalDate targetDate = LocalDate.of(2026, 4, 22);
    Path sourceFile = Files.createFile(tempDir.resolve("application.2026-04-22.log"));
    given(s3Client.putObject(any(PutObjectRequest.class), any(Path.class)))
        .willReturn(PutObjectResponse.builder().build());

    // when
    LogUploadResult result = logUploadService.uploadDailyLog(targetDate);

    // then
    ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client, times(1)).putObject(requestCaptor.capture(), any(Path.class));
    PutObjectRequest request = requestCaptor.getValue();

    assertThat(request.bucket()).isEqualTo("test-log-bucket");
    assertThat(request.key()).isEqualTo("logs/application.2026-04-22.log");
    assertThat(result.status()).isEqualTo(LogUploadResult.Status.UPLOADED_TO_S3);
    assertThat(result.sourcePath()).isEqualTo(sourceFile.toString());
    assertThat(result.s3Key()).isEqualTo("logs/application.2026-04-22.log");
  }

  @Test
  @DisplayName("업로드 실패 시 최대 재시도 횟수만큼 시도하고 실패를 반환한다")
  void should_retry_until_max_retries_and_return_failure() throws IOException {
    // given
    LocalDate targetDate = LocalDate.of(2026, 4, 22);
    Files.createFile(tempDir.resolve("application.2026-04-22.log"));
    given(s3Client.putObject(any(PutObjectRequest.class), any(Path.class)))
        .willThrow(new RuntimeException("s3 upload failed"));

    // when
    LogUploadResult result = logUploadService.uploadDailyLog(targetDate);

    // then
    verify(s3Client, times(3)).putObject(any(PutObjectRequest.class), any(Path.class));
    assertThat(result.status()).isEqualTo(LogUploadResult.Status.UPLOAD_FAILED);
    assertThat(result.s3Key()).isEqualTo("logs/application.2026-04-22.log");
  }

  @Test
  @DisplayName("첫 업로드가 실패해도 재시도로 성공하면 성공을 반환한다")
  void should_succeed_when_retry_succeeds() throws IOException {
    // given
    LocalDate targetDate = LocalDate.of(2026, 4, 22);
    Files.createFile(tempDir.resolve("application.2026-04-22.log"));
    given(s3Client.putObject(any(PutObjectRequest.class), any(Path.class)))
        .willThrow(new RuntimeException("temporary failure"))
        .willReturn(PutObjectResponse.builder().build());

    // when
    LogUploadResult result = logUploadService.uploadDailyLog(targetDate);

    // then
    verify(s3Client, times(2)).putObject(any(PutObjectRequest.class), any(Path.class));
    assertThat(result.status()).isEqualTo(LogUploadResult.Status.UPLOADED_TO_S3);
    assertThat(result.s3Key()).isEqualTo("logs/application.2026-04-22.log");
  }
}
