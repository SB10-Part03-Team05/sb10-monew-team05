package com.codeit.monew.domain.article.service;

import com.codeit.monew.domain.article.dto.backup.ArticleBackupDto;
import com.codeit.monew.domain.article.dto.response.ArticleRestoreResultDto;
import com.codeit.monew.domain.article.repository.ArticleInterestRepository;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.infra.storage.s3.S3ArticleBackupFileStorage;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ArticleRestoreService {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final ArticleRepository articleRepository;
  private final ArticleInterestRepository articleInterestRepository;
  private final InterestRepository interestRepository;

  private final S3ArticleBackupFileStorage s3ArticleBackupFileStorage;

  public List<ArticleRestoreResultDto> restore(LocalDateTime from, LocalDateTime to) {
    log.info("[ARTICLE_RESTORE] 뉴스 기사 복원 시작: from={}, to={}", from, to);

    // S3 조회 시작 시간
    LocalDate fromDate = from.atZone(KST).toLocalDate();
    // S3 조회 종료 시간
    LocalDate toDate = to.atZone(KST).toLocalDate();

    List<ArticleRestoreResultDto> restoreArticleList = new ArrayList<>();

    // S3에서 요청한 날짜 범위에 해당하는 백업 뉴스 기사 가져옴
    while (!fromDate.isAfter(toDate)) {
      LocalDate restoreDate = fromDate;

      List<ArticleBackupDto> backupArticleList = s3ArticleBackupFileStorage
          .readArticles(restoreDate);

      restoreArticleList.add(restoreArticle(restoreDate, backupArticleList));

      fromDate = fromDate.plusDays(1);
    }

    log.info("[ARTICLE_RESTORE] 뉴스 기사 복원 완료: from={}, to={}, count={}", from, to,
        restoreArticleList.size());

    return restoreArticleList;
  }

  private ArticleRestoreResultDto restoreArticle(
      LocalDate restoreDate,
      List<ArticleBackupDto> backupArticleList
  ) {
    Instant restoreInstant = restoreDate.atStartOfDay(KST).toInstant();

    // 해당 기간 내에 백업된 뉴스 기사가 없을 경우, 빈 restoredArticleIds
    if (backupArticleList.isEmpty()) {
      return new ArticleRestoreResultDto(
          restoreInstant,
          List.of(),
          0
      );
    }

    // DB 조회 시작 시간
    Instant fromInstant = restoreDate.atStartOfDay(KST).toInstant();
    // DB 조회 종료 시간
    Instant toInstant = restoreDate.plusDays(1).atStartOfDay(KST).toInstant();

    // from ... to 사이의 뉴스 기사 id를 가져옴
    List<UUID> savedArticleIdsInDB = articleRepository
        .findIdsByPublishDateBetween(fromInstant, toInstant);

    // 백업된 뉴스 기사와 DB에 저장된 뉴스 기사 비교하여 누락된 뉴스 기사 DTO List 생성
    List<ArticleBackupDto> articlesToBeRestored = backupArticleList.stream()
        .filter(dto -> !savedArticleIdsInDB.contains(dto.id()))
        .toList();

    // DB에 누락된 뉴스 기사 복구
    articlesToBeRestored.forEach(dto -> {
          articleRepository.insertRestoredArticle(
              dto.id(),
              dto.source().toString(),
              dto.sourceUrl(),
              dto.title(),
              dto.publishDate(),
              dto.summary(),
              dto.createdAt(),
              dto.updatedAt()
          );

          restoreArticleInterests(dto);
        }
    );

    List<UUID> restoredArticleIds = articlesToBeRestored.stream()
        .map(dto -> dto.id())
        .toList();

    return new ArticleRestoreResultDto(
        restoreInstant,
        restoredArticleIds,
        restoredArticleIds.size()
    );
  }

  private void restoreArticleInterests(ArticleBackupDto dto) {
    List<UUID> interestIds = dto.interestIds();

    if (interestIds == null || interestIds.isEmpty()) {
      return;
    }

    List<UUID> existingInterestIds = interestRepository.findExistingInterestIds(interestIds);

    existingInterestIds.forEach(id ->
        articleInterestRepository.insertArticleInterestIfNotExists(dto.id(), id)
    );
  }
}
