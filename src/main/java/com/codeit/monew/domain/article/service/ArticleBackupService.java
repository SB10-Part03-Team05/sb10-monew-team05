package com.codeit.monew.domain.article.service;

import com.codeit.monew.domain.article.dto.backup.ArticleBackupDto;
import com.codeit.monew.domain.article.mapper.ArticleBackupMapper;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.global.exception.common.InvalidParameterException;
import com.codeit.monew.infra.storage.s3.S3ArticleBackupFileStorage;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ArticleBackupService {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final ArticleRepository articleRepository;
  private final ArticleBackupMapper articleBackupMapper;

  private final S3ArticleBackupFileStorage s3ArticleBackupFileStorage;

  public void backup(LocalDate backupDate) {
    // backupDate 검증
    validateBackupDate(backupDate);

    // backupDate 시작 시간을 기준으로 한국 시간 기준으로 변경 후 Instant로 변환
    Instant from = backupDate.atStartOfDay(KST).toInstant(); // 2026-04-26 00:00:00 KST => Instant
    // backupDate 시작 시간을 기준으로 하루 뒤의 시각을 한국 시간 기준으로 변경 후 Instant로 변환
    Instant to = backupDate.plusDays(1).atStartOfDay(KST).toInstant();

    log.info("[ARTICLE_BACKUP] 뉴스 기사 백업 시작: backupDate={}, from={}, to={}", backupDate, from, to);

    // publishDate 이상(>=) And publishDate 미만(<)인 뉴스 기사를 가져옴
    List<ArticleBackupDto> backupData = articleRepository
        .findAllWithInterests(from, to)
        .stream()
        .map(article -> articleBackupMapper.toDto(article))
        .toList();

    // S3에 저장할 백업 파일 경로 생성
    String key = createBackupKey(backupDate);

    // S3에 업로드
    s3ArticleBackupFileStorage.upload(key, backupData);

    log.info("[ARTICLE_BACKUP] 뉴스 기사 백업 완료: backupDate={}, from={}, to={}, count={}, key={}",
        backupDate, from, to, backupData.size(), key);
  }

  // backupDate 검증 메서드
  private void validateBackupDate(LocalDate backupDate) {
    if (backupDate == null) {
      // 예외 처리
      throw new InvalidParameterException("backupDate", null);
    }
  }

  // S3에 저장할 백업 파일 경로 생성 메서드
  private String createBackupKey(LocalDate backupDate) {
    String backupKeyPrefix = "backups/articles/";

    // backups/articles/2026-04-26/articles.json
    return backupKeyPrefix + backupDate + "/articles.json";
  }
}
