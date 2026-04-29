package com.codeit.monew.infra.storage.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.dto.backup.ArticleBackupDto;
import com.codeit.monew.global.config.AwsProperties;
import com.codeit.monew.global.exception.article.ArticleFileReadFailedException;
import com.codeit.monew.global.exception.article.ArticleFileSaveFailedException;
import com.codeit.monew.global.exception.aws.AwsServerConnectFailedException;
import com.codeit.monew.global.exception.common.JsonParserFailedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class S3ArticleBackupFileStorageTest {

  @Mock
  private S3Client s3Client;

  private AwsProperties createAwsProperties(String bucket) {
    AwsProperties awsProperties = new AwsProperties();
    awsProperties.setBucket(bucket);
    return awsProperties;
  }

  private ArticleBackupDto createArticleBackupDto() {
    Instant now = Instant.parse("2026-04-26T00:00:00Z");

    return new ArticleBackupDto(UUID.randomUUID(), ArticleSource.NAVER, "https://naver.com/news/1",
        "title", now, "summary", now, now, null, List.of());
  }

  private ResponseInputStream<GetObjectResponse> createdResponseInputStream(String content) {
    return new ResponseInputStream<>(
        GetObjectResponse.builder().build(),
        AbortableInputStream.create(
            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        )
    );
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
          objectMapper);
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
      ObjectMapper objectMapper = mock(ObjectMapper.class);
      ObjectWriter objectWriter = mock(ObjectWriter.class);
      S3ArticleBackupFileStorage storage = new S3ArticleBackupFileStorage(awsProperties, s3Client,
          objectMapper);

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
    void fail_upload_when_s3_exception() {
      // given
      AwsProperties awsProperties = createAwsProperties("monew-backup-bucket");
      ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
      S3ArticleBackupFileStorage storage = new S3ArticleBackupFileStorage(awsProperties, s3Client,
          objectMapper);

      given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
          .willThrow(S3Exception.builder().message("s3 failed").build());

      // when & then
      assertThrows(ArticleFileSaveFailedException.class,
          () -> storage.upload("backups/articles/2026-04-26/articles.json",
              List.of(createArticleBackupDto())));
    }

    @Test
    @DisplayName("AWS SDK 클라이언트 오류 시 503 상태코드와 AwsServerConnectFailedException이 발생한다.")
    void fail_upload_when_aws_sdk_client_exception_occurred() {
      // given
      AwsProperties awsProperties = createAwsProperties("monew-backup-bucket");
      ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
      S3ArticleBackupFileStorage storage = new S3ArticleBackupFileStorage(awsProperties, s3Client,
          objectMapper);

      given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
          .willThrow(SdkClientException.builder().message("aws client failed").build());

      // when & then
      assertThrows(AwsServerConnectFailedException.class,
          () -> storage.upload("backups/articles/2026-04-26/articles.json",
              List.of(createArticleBackupDto())));
    }
  }

  @Nested
  @DisplayName("S3에 저장된 뉴스 기사 백업 파일 읽기")
  class readArticles {

    @Test
    @DisplayName("날짜에 해당하는 S3 백업 JSON 파일을 읽어 ArticleBackupDto 목록을 반환")
    void success_read_article_list_by_date() throws JsonProcessingException {
      // given
      AwsProperties awsProperties = createAwsProperties("monew-backup-bucket");
      ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
      S3ArticleBackupFileStorage storage = new S3ArticleBackupFileStorage(awsProperties, s3Client,
          objectMapper);

      LocalDate date = LocalDate.of(2026, 4, 26);
      String key = "backups/articles/" + date + "/articles.json";

      ArticleBackupDto expectedArticleBackupDto = createArticleBackupDto();
      String json = objectMapper.writeValueAsString(List.of(expectedArticleBackupDto));

      given(s3Client.getObject(any(GetObjectRequest.class)))
          .willReturn(createdResponseInputStream(json));

      // when
      List<ArticleBackupDto> result = storage.readArticles(date);

      // then
      assertEquals(1, result.size());
      assertEquals(expectedArticleBackupDto.id(), result.get(0).id());

      ArgumentCaptor<GetObjectRequest> requestCaptor =
          ArgumentCaptor.forClass(GetObjectRequest.class);
      verify(s3Client).getObject(requestCaptor.capture());

      GetObjectRequest request = requestCaptor.getValue();
      assertEquals("monew-backup-bucket", request.bucket());
      assertEquals(key, request.key());
    }

    @Test
    @DisplayName("날짜에 해당하는 S3 백업 JSON 파일이 없으면 빈 리스트를 반환")
    void success_read_empty_article_list_by_date() {
      // given
      AwsProperties awsProperties = createAwsProperties("monew-backup-bucket");
      ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
      S3ArticleBackupFileStorage storage = new S3ArticleBackupFileStorage(awsProperties, s3Client,
          objectMapper);

      LocalDate date = LocalDate.of(2026, 4, 26);
      String key = "backups/articles/" + date + "/articles.json";

      given(s3Client.getObject(any(GetObjectRequest.class)))
          .willThrow(NoSuchKeyException.builder().message("not found").build());

      // when
      List<ArticleBackupDto> result = storage.readArticles(date);

      // then
      assertTrue(result.isEmpty());

      ArgumentCaptor<GetObjectRequest> requestCaptor =
          ArgumentCaptor.forClass(GetObjectRequest.class);
      verify(s3Client).getObject(requestCaptor.capture());

      GetObjectRequest request = requestCaptor.getValue();
      assertEquals("monew-backup-bucket", request.bucket());
      assertEquals(key, request.key());
    }

    @Test
    @DisplayName("날짜에 해당하는 S3 백업 JSON 파일 형식이 잘못되면 500 상태코드와 JsonParserFailedException 예외가 발생한다.")
    void fail_read_article_list_by_date_when_json_processing_failed() {
      // given
      AwsProperties awsProperties = createAwsProperties("monew-backup-bucket");
      ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
      S3ArticleBackupFileStorage storage = new S3ArticleBackupFileStorage(awsProperties, s3Client,
          objectMapper);

      LocalDate date = LocalDate.of(2026, 4, 26);
      String json = "{ j s o n";

      given(s3Client.getObject(any(GetObjectRequest.class)))
          .willReturn(createdResponseInputStream(json));

      // when, then
      assertThrows(JsonParserFailedException.class,
          () -> storage.readArticles(date));

      ArgumentCaptor<GetObjectRequest> requestCaptor =
          ArgumentCaptor.forClass(GetObjectRequest.class);
      verify(s3Client).getObject(requestCaptor.capture());
    }

    @Test
    @DisplayName("날짜에 해당하는 S3 백업 JSON 파일 읽기 실패 시 503 상태코드와 ArticleFileReadFailedException 예외가 발생한다.")
    void fail_read_article_list_by_date_when_s3_exception() {
      // given
      AwsProperties awsProperties = createAwsProperties("monew-backup-bucket");
      ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
      S3ArticleBackupFileStorage storage = new S3ArticleBackupFileStorage(awsProperties, s3Client,
          objectMapper);

      LocalDate date = LocalDate.of(2026, 4, 26);

      given(s3Client.getObject(any(GetObjectRequest.class)))
          .willThrow(S3Exception.builder().message("s3 failed").build());

      // when, then
      assertThrows(ArticleFileReadFailedException.class,
          () -> storage.readArticles(date));

      ArgumentCaptor<GetObjectRequest> requestCaptor =
          ArgumentCaptor.forClass(GetObjectRequest.class);
      verify(s3Client).getObject(requestCaptor.capture());
    }

    @Test
    @DisplayName("AWS SDK 클라이언트 오류 시 503 상태코드와 AwsServerConnectFailedException 예외가 발생한다.")
    void fail_read_article_list_by_date_when_aws_sdk_client_exception() {
      // given
      AwsProperties awsProperties = createAwsProperties("monew-backup-bucket");
      ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
      S3ArticleBackupFileStorage storage = new S3ArticleBackupFileStorage(awsProperties, s3Client,
          objectMapper);

      LocalDate date = LocalDate.of(2026, 4, 26);

      given(s3Client.getObject(any(GetObjectRequest.class)))
          .willThrow(SdkClientException.builder().message("aws client failed").build());

      // when, then
      assertThrows(AwsServerConnectFailedException.class,
          () -> storage.readArticles(date));

      ArgumentCaptor<GetObjectRequest> requestCaptor =
          ArgumentCaptor.forClass(GetObjectRequest.class);
      verify(s3Client).getObject(requestCaptor.capture());
    }
  }
}
