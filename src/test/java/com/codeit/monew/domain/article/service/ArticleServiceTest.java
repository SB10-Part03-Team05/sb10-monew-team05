package com.codeit.monew.domain.article.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.dto.response.ArticleDto;
import com.codeit.monew.domain.article.dto.response.CursorPageResponseArticleDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.entity.type.ArticleDirection;
import com.codeit.monew.domain.article.entity.type.ArticleOrderBy;
import com.codeit.monew.domain.article.mapper.ArticleMapper;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.article.repository.ArticleViewHistoryRepository;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.exception.article.ArticleNotFoundException;
import com.codeit.monew.global.exception.interest.InterestNotFoundException;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
  private InterestRepository interestRepository;

  @Mock
  private ArticleMapper articleMapper;

  @InjectMocks
  private ArticleService articleService;

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

  private Interest createInterest(UUID interestId, String name) {
    Interest interest = Interest.create("삼성");

    if (interestId == null) {
      ReflectionTestUtils.setField(interest, "id", UUID.randomUUID());
    } else {
      ReflectionTestUtils.setField(interest, "id", interestId);
    }

    return interest;
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

  @Nested
  @DisplayName("뉴스 기사 출처 목록 조회 테스트")
  class getSource {

    @Test
    @DisplayName("DB에 저장된 뉴스 기사의 출처 목록을 조회할 수 있다.")
    void success_get_article_source() {
      // given(준비)
      List<ArticleSource> expectedSources = List.of(ArticleSource.CHOSUN, ArticleSource.NAVER,
          ArticleSource.YEONHAP);

      given(articleService.getSources()).willReturn(expectedSources);

      // when(실행)
      List<ArticleSource> result = articleService.getSources();

      // then(검증)
      assertEquals(expectedSources, result);

      verify(articleRepository).findDistinctSource();
    }
  }

  @Nested
  @DisplayName("뉴스 기사 목록 조회 테스트")
  class search {

    private ArticleSearchRequest request;
    private UUID interestId;
    private UUID requestUserId;

    @BeforeEach
    void setUp() {
      interestId = UUID.randomUUID();
      requestUserId = UUID.randomUUID();

      request = new ArticleSearchRequest();
      request.setInterestId(interestId);
      request.setOrderBy(ArticleOrderBy.publishDate);
      request.setDirection(ArticleDirection.DESC);
      request.setLimit(3);
    }

    @Test
    @DisplayName("여러 쿼리 파라미터로 뉴스 기사 목록 조회할 수 있다.")
    void success_search_article_list() {
      // given(준비)
      Interest interest = createInterest(interestId, "삼성");
      User user = createUser(requestUserId, "test@email.com", "testNickname", "testPassword");

      Article article1 = createArticle(null, ArticleSource.NAVER, "https://naver.com", "testTitle1",
          Instant.now(), "testSummary1");
      Article article2 = createArticle(null, ArticleSource.YEONHAP, "https://yeonhap.com",
          "testTitle2", Instant.parse("2026-04-17T09:12:15Z"), "testSummary2");

      ArticleDto articleDto1 = createArticleDto(article1, 5, 6, true);
      ArticleDto articleDto2 = createArticleDto(article2, 3, 7, false);

      CursorPageResponseArticleDto expectedResponseDto = new CursorPageResponseArticleDto(
          List.of(articleDto1, articleDto2),
          "2026-04-17T09:12:15Z",
          Instant.parse("2026-04-17T09:12:15Z"),
          2,
          2,
          false
      );

      given(userRepository.findByIdAndDeletedAtIsNull(requestUserId)).willReturn(Optional.of(user));
      given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
      given(articleRepository.searchArticleList(request, requestUserId)).willReturn(
          expectedResponseDto);

      // when(실행)
      CursorPageResponseArticleDto result = articleService.search(request, requestUserId);

      // then(검증)
      assertEquals(expectedResponseDto, result);

      verify(userRepository).findByIdAndDeletedAtIsNull(requestUserId);
      verify(interestRepository).findById(interestId);
      verify(articleRepository).searchArticleList(request, requestUserId);
    }

    @Test
    @DisplayName("뉴스 기사 단건 조회 시 존재하지 않거나 논리 삭제된 사용자 ID가 조회되면 UserNotFound 예외가 발생한다.")
    void fail_search_article_list_when_user_not_found() {
      // given(준비)
      given(userRepository.findByIdAndDeletedAtIsNull(requestUserId)).willReturn(Optional.empty());

      // when(실행), then(검증)
      assertThrows(UserNotFoundException.class,
          () -> articleService.search(request, requestUserId));

      verify(userRepository).findByIdAndDeletedAtIsNull(requestUserId);
      verify(interestRepository, never()).findById(any());
      verify(articleRepository, never()).searchArticleList(any(ArticleSearchRequest.class), any());
    }

    @Test
    @DisplayName("뉴스 기사 단건 조회 시 존재하지 않은 관심사 ID가 조회되면 InterestNotFound 예외가 발생한다.")
    void fail_search_article_list_when_interest_not_found() {
      // given(준비)
      User user = createUser(requestUserId, "test@email.com", "testNickname", "testPassword");

      request.setInterestId(interestId);

      given(userRepository.findByIdAndDeletedAtIsNull(requestUserId)).willReturn(Optional.of(user));
      given(interestRepository.findById(interestId)).willReturn(Optional.empty());

      // when(실행), then(검증)
      assertThrows(InterestNotFoundException.class,
          () -> articleService.search(request, requestUserId));

      verify(userRepository).findByIdAndDeletedAtIsNull(requestUserId);
      verify(interestRepository).findById(any());
      verify(articleRepository, never()).searchArticleList(any(ArticleSearchRequest.class), any());
    }
  }
}