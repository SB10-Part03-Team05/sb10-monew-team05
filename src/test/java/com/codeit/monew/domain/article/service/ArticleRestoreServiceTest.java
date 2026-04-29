package com.codeit.monew.domain.article.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.dto.backup.ArticleBackupDto;
import com.codeit.monew.domain.article.dto.response.ArticleRestoreResultDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.infra.storage.s3.S3ArticleBackupFileStorage;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleRestoreServiceTest {

  @Mock
  private ArticleRepository articleRepository;

  @Mock
  private S3ArticleBackupFileStorage s3ArticleBackupFileStorage;

  @InjectMocks
  private ArticleRestoreService articleRestoreService;

  private Article createArticle(UUID articleId, ArticleSource source, String sourceUrl,
      String title, Instant publishDate, String summary) {
    Article article = Article.createArticle(source, sourceUrl, title, publishDate, summary);

    if (articleId == null) {
      ReflectionTestUtils.setField(article, "id", UUID.randomUUID());
    } else {
      ReflectionTestUtils.setField(article, "id", articleId);
    }

    return article;
  }

  private ArticleBackupDto createArticleBackupDto(Article article) {
    return new ArticleBackupDto(article.getId(), article.getSource(), article.getSourceUrl(),
        article.getTitle(), article.getPublishDate(), article.getSummary(), Instant.now(),
        Instant.now(), Instant.now(), List.of());
  }

  private ArticleRestoreResultDto createArticleRestoreResultDto(Instant restoreDate,
      List<UUID> restoreArticleIds, long restoredArticleCount) {

    return new ArticleRestoreResultDto(restoreDate, restoreArticleIds, restoredArticleCount);
  }

  @Nested
  @DisplayName("뉴스 기사 복구 테스트")
  class restoreArticle {

    ZoneId KST;
    LocalDateTime from;
    LocalDateTime to;
    LocalDate fromDate;
    LocalDate toDate;

    @BeforeEach
    void backupSetup() {
      KST = ZoneId.of("Asia/Seoul");
      from = LocalDateTime.of(2026, Month.APRIL, 1, 0, 0, 0);
      to = LocalDateTime.of(2026, Month.APRIL, 2, 23, 59, 59);
      fromDate = from.atZone(KST).toLocalDate();
      toDate = to.atZone(KST).toLocalDate();
    }

    @Test
    @DisplayName("요청한 날짜 범위에서 DB에 유실된 뉴스 기사를 복원할 수 있다.")
    void success_restore() {
      // give
      Instant savedDate = from.atZone(KST).toInstant();

      Article article = createArticle(null, ArticleSource.NAVER, "https://naver.com", "title",
          savedDate, "summary");
      ArticleBackupDto articleBackupDto = createArticleBackupDto(article);

      ArticleRestoreResultDto articleRestoreResultDto1 = createArticleRestoreResultDto(
          fromDate.atStartOfDay(KST).toInstant(),
          List.of(articleBackupDto.id()),
          1L
      );
      ArticleRestoreResultDto articleRestoreResultDto2 = createArticleRestoreResultDto(
          toDate.atStartOfDay(KST).toInstant(),
          List.of(articleBackupDto.id()),
          1L
      );

      List<ArticleRestoreResultDto> expectedRestoreArticleList = List.of(
          articleRestoreResultDto1,
          articleRestoreResultDto2
      );

      given(s3ArticleBackupFileStorage.readArticles(any()))
          .willReturn(List.of(articleBackupDto));
      given(articleRepository.findIdsByPublishDateBetween(any(), any()))
          .willReturn(List.of());

      // when
      List<ArticleRestoreResultDto> result = articleRestoreService.restore(from, to);

      // given
      assertEquals(expectedRestoreArticleList, result);

      verify(s3ArticleBackupFileStorage, times(2)).readArticles(any());
      verify(articleRepository, times(2)).findIdsByPublishDateBetween(any(), any());
    }

    @Test
    @DisplayName("요청한 날짜 범위 내에 DB에 누락된 뉴스 기사가 없을 경우, 빈 뉴스 기사 ID 리스트를 포함해 반환한다.")
    void success_restore_when_no_missing_articles() {
      // give
      ArticleRestoreResultDto articleRestoreResultDto1 = createArticleRestoreResultDto(
          fromDate.atStartOfDay(KST).toInstant(),
          List.of(),
          0L
      );
      ArticleRestoreResultDto articleRestoreResultDto2 = createArticleRestoreResultDto(
          toDate.atStartOfDay(KST).toInstant(),
          List.of(),
          0L
      );

      List<ArticleRestoreResultDto> expectedRestoreArticleList = List.of(
          articleRestoreResultDto1,
          articleRestoreResultDto2
      );

      given(s3ArticleBackupFileStorage.readArticles(any()))
          .willReturn(List.of());

      // when
      List<ArticleRestoreResultDto> result = articleRestoreService.restore(from, to);

      // given
      assertEquals(expectedRestoreArticleList, result);

      verify(s3ArticleBackupFileStorage, times(2)).readArticles(any());
      verify(articleRepository, never()).findIdsByPublishDateBetween(any(), any());
    }
  }
}