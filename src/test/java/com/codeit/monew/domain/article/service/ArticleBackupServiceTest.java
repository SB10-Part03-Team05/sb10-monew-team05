package com.codeit.monew.domain.article.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.dto.backup.ArticleBackupDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.mapper.ArticleBackupMapper;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.global.exception.common.InvalidParameterException;
import com.codeit.monew.infra.storage.s3.S3ArticleBackupFileStorage;
import java.time.Instant;
import java.time.LocalDate;
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
class ArticleBackupServiceTest {

  @Mock
  private ArticleRepository articleRepository;

  @Mock
  private ArticleBackupMapper articleBackupMapper;

  @Mock
  private S3ArticleBackupFileStorage s3ArticleBackupFileStorage;

  @InjectMocks
  private ArticleBackupService articleBackupService;

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

  @Nested
  @DisplayName("뉴스 기사 백업 테스트")
  class backup {

    LocalDate backupDate;
    ZoneId KST;
    Instant from;
    Instant to;

    @BeforeEach
    void backupSetup() {
      backupDate = LocalDate.now();
      KST = ZoneId.of("Asia/Seoul");
      from = backupDate.atStartOfDay(KST).toInstant();
      to = backupDate.plusDays(1).atStartOfDay(KST).toInstant();
    }

    @Test
    @DisplayName("백업 날짜로 발행된 뉴스 기사를 S3에 백업할 수 있다.")
    void success_backup() {
      // given(준비)
      Article article = createArticle(null, ArticleSource.NAVER, "https://naver.com", "title",
          Instant.now(), "summary");
      ArticleBackupDto articleDto = createArticleBackupDto(article);
      String key = "backups/articles/" + backupDate + "/articles.json";
      List<ArticleBackupDto> expectedBackupData = List.of(articleDto);

      given(articleRepository
          .findAllWithInterests(from, to))
          .willReturn(List.of(article));
      given(articleBackupMapper.toDto(article)).willReturn(articleDto);

      // when(실행)
      articleBackupService.backup(backupDate);

      // then(검증)
      verify(articleRepository)
          .findAllWithInterests(from, to);
      verify(articleBackupMapper).toDto(article);
      verify(s3ArticleBackupFileStorage).upload(key, expectedBackupData);
    }

    @Test
    @DisplayName("백업 날짜(backupDate)가 null이면 400 상태코드와 InvalidParameterException 예외가 발생한다.")
    void fail_backup_when_backupDate_is_null() {
      // given(준비)

      // when(실행), then(검증)
      assertThrows(InvalidParameterException.class,
          () -> articleBackupService.backup(null));
      verify(articleRepository, never())
          .findAllWithInterests(from, to);
      verify(articleBackupMapper, never()).toDto(any(Article.class));
      verify(s3ArticleBackupFileStorage, never()).upload(any(), any());
    }
  }
}