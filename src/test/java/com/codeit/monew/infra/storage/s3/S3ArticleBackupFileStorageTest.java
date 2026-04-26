package com.codeit.monew.infra.storage.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.dto.backup.ArticleBackupDto;
import com.codeit.monew.global.config.AwsProperties;
import com.codeit.monew.global.exception.article.ArticleFileSaveFailedException;
import com.codeit.monew.global.exception.aws.AwsServerConnectFailedException;
import com.codeit.monew.global.exception.common.JsonParserFailedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ExtendWith(MockitoExtension.class)
class S3ArticleBackupFileStorageTest {

  @Mock
  private S3Client s3Client;

  @Mock
  private S3Presigner s3Presigner;

  private AwsProperties createAwsProperties(String bucket) {
    AwsProperties awsProperties = new AwsProperties();
    awsProperties.setBucket(bucket);
    return awsProperties;
  }

  private ArticleBackupDto createArticleBackupDto() {
    Instant now = Instant.parse("2026-04-26T00:00:00Z");

    return new ArticleBackupDto(UUID.randomUUID(), ArticleSource.NAVER, "https://naver.com/news/1",
        "title", now, "summary", now, now, null);
  }

  @Nested
  @DisplayName("뉴스 기사 백업 파일 S3 저장")
  class upload {

    @Test
    @DisplayName("백업 DTO 목록을 JSON 파일로 S3에 업로드한다.")
    void success_upload_article_backup_file() {
      // given
      AwsProperties awsProperties = createAwsProperties("monew-backup-bucket");
      ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
      S3ArticleBackupFileStorage storage = new S3ArticleBackupFileStorage(awsProperties, s3Client,
          s3Presigner, objectMapper);
      String key = "backups/articles/2026-04-26/articles.json";
      List<ArticleBackupDto> articleList = List.of(createArticleBackupDto());

      // when
      storage.upload(key, articleList);

      // then
      ArgumentCaptor<PutObjectRequest> requestCaptor =
          ArgumentCaptor.forClass(PutObjectRequest.class);
      ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

      verify(s3Client).putObject(requestCaptor.capture(), bodyCaptor.capture());

      PutObjectRequest request = requestCaptor.getValue();
      assertEquals("monew-backup-bucket", request.bucket());
      assertEquals(key, request.key());
      assertEquals("application/json", request.contentType());
      assertTrue(bodyCaptor.getValue().optionalContentLength().orElse(0L) > 0);
    }

    @Test
    @DisplayName("JSON 직렬화 실패 시 500 상태코드와 JsonParserFailedException이 발생한다.")
    void fail_upload_when_json_processing_failed() throws Exception {
      // given
      AwsProperties awsProperties = createAwsProperties("monew-backup-bucket");
      ObjectMapper objectMapper = org.mockito.Mockito.mock(ObjectMapper.class);
      ObjectWriter objectWriter = org.mockito.Mockito.mock(ObjectWriter.class);
      S3ArticleBackupFileStorage storage = new S3ArticleBackupFileStorage(awsProperties, s3Client,
          s3Presigner, objectMapper);

      given(objectMapper.writerWithDefaultPrettyPrinter()).willReturn(objectWriter);
      given(objectWriter.writeValueAsBytes(any()))
          .willThrow(new JsonProcessingException("json failed") {
          });

      // when & then
      assertThrows(JsonParserFailedException.class,
          () -> storage.upload("backups/articles/2026-04-26/articles.json", List.of()));
      verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("S3 업로드 실패 시 503 상태코드와 ArticleFileSaveFailedException이 발생한다.")
    void fail_upload_when_s3_exception_occurred() {
      // given
      AwsProperties awsProperties = createAwsProperties("monew-backup-bucket");
      ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
      S3ArticleBackupFileStorage storage = new S3ArticleBackupFileStorage(awsProperties, s3Client,
          s3Presigner, objectMapper);

      given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
          .willThrow(S3Exception.builder().message("s3 failed").build());

      // when & then
      assertThrows(ArticleFileSaveFailedException.class,
          () -> storage.upload("backups/articles/2026-04-26/articles.json",
              List.of(createArticleBackupDto())));
    }

    @Test
    @DisplayName("AWS SDK 클라이언트 오류 시 503 상태코드와 AwsServerConnectFailedException이 발생한다.")
    void fail_upload_when_sdk_client_exception_occurred() {
      // given
      AwsProperties awsProperties = createAwsProperties("monew-backup-bucket");
      ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
      S3ArticleBackupFileStorage storage = new S3ArticleBackupFileStorage(awsProperties, s3Client,
          s3Presigner, objectMapper);

      given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
          .willThrow(SdkClientException.builder().message("aws client failed").build());

      // when & then
      assertThrows(AwsServerConnectFailedException.class,
          () -> storage.upload("backups/articles/2026-04-26/articles.json",
              List.of(createArticleBackupDto())));
    }
  }
}
