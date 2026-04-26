package com.codeit.monew.infra.storage.s3;

import com.codeit.monew.domain.article.dto.backup.ArticleBackupDto;
import com.codeit.monew.global.config.AwsProperties;
import com.codeit.monew.global.exception.aws.AwsServerConnectFailedException;
import com.codeit.monew.global.exception.common.JsonParserFailedException;
import com.codeit.monew.global.exception.article.ArticleFileSaveFailedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Component
@RequiredArgsConstructor
@Slf4j
// S3 업로드/다운로드
public class S3ArticleBackupFileStorage {

  private final AwsProperties awsProperties;
  private final S3Client s3Client;
  private final S3Presigner s3Presigner;

  private final ObjectMapper objectMapper;

  @Retryable(
      retryFor = { // 아래 예외일 때 retry
          ArticleFileSaveFailedException.class,
          AwsServerConnectFailedException.class
      },
      maxAttempts = 3, // retry 횟수
      backoff = @Backoff(delay = 1000, multiplier = 2) // 재시도 사이 대기 시간
  )
  public void upload(String key, List<ArticleBackupDto> articleList) {
    try {
      log.info("[S3_ARTICLE_BACKUP_UPLOAD] 뉴스 기사 백업 S3 업로드 시작: key={}, count={}", key,
          articleList.size());

      // Java 객체를 JSON으로 변환
      byte[] jsonBytes = objectMapper
          // JSON을 사람이 읽기 좋게 줄바꿈 및 들여쓰기 추가
          .writerWithDefaultPrettyPrinter()
          // Article List를 JSON byte 배열로 변환
          .writeValueAsBytes(articleList);

      // S3 putObject request 객체 생성
      PutObjectRequest request = PutObjectRequest.builder()
          .bucket(awsProperties.getBucket())
          .key(key)
          .contentType("application/json")
          .build();

      // S3에 파일 업로드 실행
      s3Client.putObject(request, RequestBody.fromBytes(jsonBytes));

      log.info("[S3_ARTICLE_BACKUP_UPLOAD] 뉴스 기사 백업 S3 업로드 완료: key={}, count={}", key,
          articleList.size());

    } catch (JsonProcessingException e) {
      throw new JsonParserFailedException(e);
    } catch (S3Exception e) {
      throw new ArticleFileSaveFailedException(e);
    } catch (SdkClientException e) {
      throw new AwsServerConnectFailedException(e);
    }
  }
}
