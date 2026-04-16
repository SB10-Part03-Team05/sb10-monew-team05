package com.codeit.monew.domain.article.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.article.dto.ArticleDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.mapper.ArticleMapper;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.article.repository.ArticleViewHistoryRepository;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.exception.article.ArticleNotFoundException;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

  @Mock
  private ArticleRepository articleRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ArticleViewHistoryRepository articleViewHistoryRepository;

  @Mock
  private CommentRepository commentRepository;

  @Mock
  private ArticleMapper articleMapper;

  @InjectMocks
  private ArticleService articleService;

  private Article createArticle(UUID articleId, ArticleSource source, String sourceUrl,
      String title, Instant publishDate, String summary) {
    Article article = new Article(sourceUrl, source, title, publishDate, summary);

    if (articleId == null) {
      ReflectionTestUtils.setField(article, "id", UUID.randomUUID());
    } else {
      ReflectionTestUtils.setField(article, "id", articleId);
    }

    return article;
  }

  private ArticleDto createArticleDto(Article article, long commentCount, long viewCount,
      boolean viewedByMe) {
    return new ArticleDto(article.getId(), article.getSource(), article.getSourceUrl(),
        article.getTitle(), article.getPublishDate(), article.getSummary(), commentCount, viewCount,
        viewedByMe);
  }

  private User createUser(UUID userId, String email, String nickname, String password) {
    User user = new User(email, nickname, password);

    if (userId == null) {
      ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    } else {
      ReflectionTestUtils.setField(user, "id", userId);
    }

    return user;
  }

  @Nested
  @DisplayName("뉴스 기사 단건 조회 테스트")
  class getArticle {

    @Test
    @DisplayName("뉴스 기사 ID로 뉴스 기사 단건 조회할 수 있다.")
    void success_get_article_by_articleId() {
      // given(준비)
      UUID articleId = UUID.randomUUID();
      UUID requestUserId = UUID.randomUUID();
      User user = createUser(requestUserId, "test@email.com", "testNickname", "testPassword");
      Article article = createArticle(articleId, ArticleSource.NAVER, "https://naver.com", "title",
          Instant.now(), "summary");
      ArticleDto expectedArticleDto = createArticleDto(article, 3, 5, true);

      given(userRepository.findByIdAndDeletedAtIsNull(requestUserId)).willReturn(Optional.of(user));
      given(articleRepository.findByIdAndDeletedAtIsNull(articleId)).willReturn(
          Optional.of(article));
      given(commentRepository.countByArticleIdAndDeletedAtIsNull(articleId)).willReturn(3L);
      given(articleViewHistoryRepository.countByArticleId(articleId)).willReturn(5L);
      given(articleViewHistoryRepository.existsByArticleIdAndUserId(articleId,
          requestUserId)).willReturn(true);
      given(articleMapper.toDto(article, 3, 5, true)).willReturn(expectedArticleDto);

      // when(실행)
      ArticleDto result = articleService.getArticle(articleId, requestUserId);

      // then(검증)
      assertEquals(expectedArticleDto, result);
      assertEquals(expectedArticleDto.id(), result.id());
      assertEquals(expectedArticleDto.source(), result.source());
      assertEquals(expectedArticleDto.title(), result.title());

      verify(userRepository).findByIdAndDeletedAtIsNull(requestUserId);
      verify(articleRepository).findByIdAndDeletedAtIsNull(articleId);
      verify(commentRepository).countByArticleIdAndDeletedAtIsNull(articleId);
      verify(articleViewHistoryRepository).countByArticleId(articleId);
      verify(articleViewHistoryRepository).existsByArticleIdAndUserId(articleId, requestUserId);
      verify(articleMapper).toDto(article, 3, 5, true);
    }

    @Test
    @DisplayName("뉴스 기사 단건 조회 시 존재하지 않거나 논리 삭제된 사용자 ID가 조회되면 UserNotFound 예외가 발생한다.")
    void fail_get_article_by_articleId_when_user_not_found() {
      // given(준비)
      UUID articleId = UUID.randomUUID();
      UUID requestUserId = UUID.randomUUID();

      given(userRepository.findByIdAndDeletedAtIsNull(requestUserId)).willReturn(
          Optional.empty());

      // when(실행), then(검증)
      assertThrows(UserNotFoundException.class,
          () -> articleService.getArticle(articleId, requestUserId));

      verify(userRepository).findByIdAndDeletedAtIsNull(requestUserId);
      verify(articleRepository, never()).findByIdAndDeletedAtIsNull(any());
      verify(commentRepository, never()).countByArticleIdAndDeletedAtIsNull(any());
      verify(articleViewHistoryRepository, never()).countByArticleId(any());
      verify(articleViewHistoryRepository, never()).existsByArticleIdAndUserId(any(), any());
      verify(articleMapper, never()).toDto(any(Article.class), anyLong(), anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("뉴스 기사 단건 조회 시 존재하지 않거나 논리 삭제된 뉴스 기사 ID가 조회되면 ArticleNotFound 예외가 발생한다.")
    void fail_get_article_by_articleId_when_article_not_found() {
      // given(준비)
      UUID articleId = UUID.randomUUID();
      UUID requestUserId = UUID.randomUUID();
      User user = createUser(requestUserId, "test@email.com", "testNickname", "testPassword");

      given(userRepository.findByIdAndDeletedAtIsNull(requestUserId)).willReturn(Optional.of(user));
      given(articleRepository.findByIdAndDeletedAtIsNull(articleId)).willReturn(
          Optional.empty());

      // when(실행), then(검증)
      assertThrows(ArticleNotFoundException.class,
          () -> articleService.getArticle(articleId, requestUserId));

      verify(userRepository).findByIdAndDeletedAtIsNull(requestUserId);
      verify(articleRepository).findByIdAndDeletedAtIsNull(articleId);
      verify(commentRepository, never()).countByArticleIdAndDeletedAtIsNull(any());
      verify(articleViewHistoryRepository, never()).countByArticleId(any());
      verify(articleViewHistoryRepository, never()).existsByArticleIdAndUserId(any(), any());
      verify(articleMapper, never()).toDto(any(Article.class), anyLong(), anyLong(), anyBoolean());
    }
  }
}