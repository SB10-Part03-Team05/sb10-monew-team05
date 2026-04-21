package com.codeit.monew.domain.article.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.dto.response.ArticleDto;
import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.dto.response.ArticleViewDto;
import com.codeit.monew.domain.article.dto.response.CursorPageResponseArticleDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleViewHistory;
import com.codeit.monew.domain.article.entity.type.ArticleDirection;
import com.codeit.monew.domain.article.entity.type.ArticleOrderBy;
import com.codeit.monew.domain.article.service.ArticleService;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.GlobalExceptionHandler;
import com.codeit.monew.global.exception.article.ArticleNotFoundException;
import com.codeit.monew.global.exception.Interest.InterestNotFoundException;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WebMvcTest(ArticleController.class)
@Import(GlobalExceptionHandler.class)
class ArticleControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  ObjectMapper om;

  @MockitoBean
  private ArticleService articleService;

  private User createUser(UUID userId, String email, String nickname, String password) {
    User user = new User(email, nickname, password);

    if (userId == null) {
      ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    } else {
      ReflectionTestUtils.setField(user, "id", userId);
    }

    return user;
  }

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

  private ArticleViewHistory createArticleViewHistory(UUID articleHistoryId, User user,
      Article article) {

    ArticleViewHistory articleViewHistory = new ArticleViewHistory(user, article);

    if (articleHistoryId == null) {
      ReflectionTestUtils.setField(articleViewHistory, "id", UUID.randomUUID());
    } else {
      ReflectionTestUtils.setField(articleViewHistory, "id", articleHistoryId);
    }

    return articleViewHistory;
  }

  private ArticleViewDto createArticleViewDto(ArticleViewHistory articleViewHistory,
      UUID requestUserId,
      Article article, long commentCount, long viewCount) {

    return new ArticleViewDto(articleViewHistory.getId(), requestUserId,
        articleViewHistory.getCreatedAt(),
        article.getId(), article.getSource(), article.getSourceUrl(), article.getTitle(),
        article.getPublishDate(), article.getSummary(), commentCount, viewCount);
  }

  @Nested
  @DisplayName("뉴스 기사 단건 조회 API 테스트")
  class getArticle {

    @Test
    @DisplayName("뉴스 기사 단건 조회하면 200 상태코드와 뉴스 기사 정보가 반환된다.")
    void success_get_article_by_articleId() throws Exception {
      // given(준비)
      UUID requestUserId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();
      Article article = createArticle(articleId, ArticleSource.NAVER, "https://naver.com",
          "testTitle", Instant.now(), "testSummary");
      ArticleDto articleDto = createArticleDto(article, 5, 6, true);

      given(articleService.getArticle(articleId, requestUserId)).willReturn(articleDto);

      // when(실행), then(검증)
      mockMvc.perform(get("/api/articles/{articleId}", articleId)
              .header("Monew-Request-User-ID", requestUserId.toString()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(articleId.toString()))
          .andExpect(jsonPath("$.source").value(ArticleSource.NAVER.toString()));
    }

    @Test
    @DisplayName("뉴스 기사 단건 조회 시 존재하지 않거나 논리 삭제된 사용자 ID가 조회되면 404 상태 코드와 UserNotFound 예외가 발생한다.")
    void fail_get_article_by_articleId_when_user_not_found() throws Exception {
      // given(준비)
      UUID requestUserId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();

      given(articleService.getArticle(articleId, requestUserId)).willThrow(
          new UserNotFoundException(requestUserId));

      // when(실행), then(검증)
      mockMvc.perform(get("/api/articles/{articleId}", articleId)
              .header("Monew-Request-User-ID", requestUserId.toString()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.toString()))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.exceptionType").value(UserNotFoundException.class.getSimpleName()))
          .andExpect(jsonPath("$.details.userId").value(requestUserId.toString()));
    }

    @Test
    @DisplayName("뉴스 기사 단건 조회 시 존재하지 않거나 논리 삭제된 뉴스 기사 ID가 조회되면 404 상태 코드와 ArticleNotFound 예외가 발생한다.")
    void fail_get_article_by_articleId_when_article_not_found() throws Exception {
      // given(준비)
      UUID requestUserId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();

      given(articleService.getArticle(articleId, requestUserId)).willThrow(
          new ArticleNotFoundException(articleId));

      // when(실행), then(검증)
      mockMvc.perform(get("/api/articles/{articleId}", articleId)
              .header("Monew-Request-User-ID", requestUserId.toString()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value(ErrorCode.ARTICLE_NOT_FOUND.toString()))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(
              jsonPath("$.exceptionType").value(ArticleNotFoundException.class.getSimpleName()))
          .andExpect(jsonPath("$.details.articleId").value(articleId.toString()));
    }
  }

  @Nested
  @DisplayName("뉴스 기사 출처 목록 조회 API 테스트")
  class getSource {

    @Test
    @DisplayName("뉴스 기사 출처 목록 조회하면 200 상태코드와 DB에 저장된 뉴스 기사의 출처 목록이 반환된다.")
    void success_get_article_source() throws Exception {
      // given(준비)
      List<ArticleSource> sources = List.of(ArticleSource.HANKYUNG, ArticleSource.NAVER,
          ArticleSource.YONHAP);

      given(articleService.getSources()).willReturn(sources);

      // when(실행), then(검증)
      mockMvc.perform(get("/api/articles/sources"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(3)))
          .andExpect(jsonPath("$[0]").value(ArticleSource.HANKYUNG.toString()))
          .andExpect(jsonPath("$[1]").value(ArticleSource.NAVER.toString()))
          .andExpect(jsonPath("$[2]").value(ArticleSource.YONHAP.toString()));
    }
  }

  @Nested
  @DisplayName("뉴스 기사 목록 조회 API 테스트")
  class search {

    @Test
    @DisplayName("뉴스 기사 목록 조회하면 200 상태코드와 커서 페이지네이션이 적용된 뉴스 기사 목록이 반환된다.")
    void success_search_article_list() throws Exception {
      // given(준비)
      UUID requestUserId = UUID.randomUUID();
      Article article1 = createArticle(null, ArticleSource.NAVER, "https://naver.com",
          "testTitle1", Instant.now(), "testSummary1");
      Article article2 = createArticle(null, ArticleSource.YONHAP, "https://yeonhap.com",
          "testTitle2", Instant.parse("2026-04-17T09:12:15Z"), "testSummary2");
      ArticleDto articleDto1 = createArticleDto(article1, 5, 6, true);
      ArticleDto articleDto2 = createArticleDto(article2, 3, 7, false);

      CursorPageResponseArticleDto response = new CursorPageResponseArticleDto(
          List.of(articleDto1, articleDto2),
          "2026-04-17T09:12:15Z",
          Instant.parse("2026-04-17T09:12:15Z"),
          2,
          2,
          false
      );

      given(articleService.search(any(ArticleSearchRequest.class), eq(requestUserId))).willReturn(
          response);

      // when(시작), then(검증)
      mockMvc.perform(get("/api/articles")
              .param("keyword", "Spring Boot")
              .param("orderBy", ArticleOrderBy.publishDate.toString())
              .param("direction", ArticleDirection.DESC.toString())
              .param("limit", "2")
              .header("Monew-Request-User-ID", requestUserId.toString()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].id").value(articleDto1.id().toString()))
          .andExpect(jsonPath("$.content[1].id").value(articleDto2.id().toString()))
          .andExpect(jsonPath("$.size").value(2));
    }

    @Test
    @DisplayName("필수 파라미터(orderBy/direction/limit) 누락 시 400 상태코드와 MethodArgumentNotValidException 예외 발생")
    void fail_search_article_list_when_required_parameter_is_not_valid() throws Exception {
      // given(준비)
      UUID requestUserId = UUID.randomUUID();

      // when(시작), then(검증)
      mockMvc.perform(get("/api/articles")
              .param("direction", ArticleDirection.DESC.toString())
              .param("limit", "5")
              .header("Monew-Request-User-ID", requestUserId))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.exceptionType").value(
              MethodArgumentNotValidException.class.getSimpleName()));
    }

    @Test
    @DisplayName("keyword가 공백이 연속될 경우 400 상태코드와 MethodArgumentNotValidException 예외 발생")
    void fail_search_article_list_when_keyword_is_not_blank() throws Exception {
      // given(준비)
      UUID requestUserId = UUID.randomUUID();

      // when(시작), then(검증)
      mockMvc.perform(get("/api/articles")
              .param("keyword", " ")
              .param("orderBy", ArticleOrderBy.publishDate.toString())
              .param("direction", ArticleDirection.DESC.toString())
              .param("limit", "5")
              .header("Monew-Request-User-ID", requestUserId))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.exceptionType").value(
              MethodArgumentNotValidException.class.getSimpleName()));
    }

    @Test
    @DisplayName("limit이 1미만일 경우 400 상태코드와 MethodArgumentNotValidException 예외 발생")
    void fail_search_article_list_when_limit_is_less_than_1() throws Exception {
      // given(준비)
      UUID requestUserId = UUID.randomUUID();

      // when(시작), then(검증)
      mockMvc.perform(get("/api/articles")
              .param("orderBy", ArticleOrderBy.publishDate.toString())
              .param("direction", ArticleDirection.DESC.toString())
              .param("limit", "-1")
              .header("Monew-Request-User-ID", requestUserId))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.exceptionType").value(
              MethodArgumentNotValidException.class.getSimpleName()));
    }

    @Test
    @DisplayName("사용자 ID를 찾을 수 없을 경우 404 상태코드와 UserNotFoundException 예외 발생")
    void fail_search_article_list_when_userId_not_found() throws Exception {
      // given(준비)
      UUID requestUserId = UUID.randomUUID();

      given(articleService.search(any(ArticleSearchRequest.class), eq(requestUserId))).willThrow(
          new UserNotFoundException(requestUserId));

      // when(시작), then(검증)
      mockMvc.perform(get("/api/articles")
              .param("orderBy", ArticleOrderBy.publishDate.toString())
              .param("direction", ArticleDirection.DESC.toString())
              .param("limit", "2")
              .header("Monew-Request-User-ID", requestUserId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.toString()))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(
              jsonPath("$.exceptionType").value(UserNotFoundException.class.getSimpleName()));
    }

    @Test
    @DisplayName("관심사 ID를 찾을 수 없을 경우 404 상태코드와 InterestNotFoundException 예외 발생")
    void fail_search_article_list_when_interestId_not_found() throws Exception {
      // given(준비)
      UUID requestUserId = UUID.randomUUID();
      UUID interestId = UUID.randomUUID();

      given(articleService.search(any(ArticleSearchRequest.class), eq(requestUserId))).willThrow(
          new InterestNotFoundException(interestId));

      // when(시작), then(검증)
      mockMvc.perform(get("/api/articles")
              .param("orderBy", ArticleOrderBy.publishDate.toString())
              .param("direction", ArticleDirection.DESC.toString())
              .param("limit", "2")
              .header("Monew-Request-User-ID", requestUserId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value(ErrorCode.INTEREST_NOT_FOUND.toString()))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(
              jsonPath("$.exceptionType").value(InterestNotFoundException.class.getSimpleName()));
    }
  }

  @Nested
  @DisplayName("뉴스 기사 view 등록 API 테스트")
  class view {

    @Test
    @DisplayName("뉴스 기사 view가 등록되면 200 상태코드와 뉴스 기사 정보가 반환된다.")
    void success_post_article_view() throws Exception {
      // given(준비)
      UUID requestUserId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();

      User user = createUser(requestUserId, "test@email.com", "testNickname", "testPassword");
      Article article = createArticle(articleId, ArticleSource.NAVER, "https://naver.com",
          "testTitle", Instant.now(), "testSummary");
      ArticleViewHistory articleViewHistory = createArticleViewHistory(null, user, article);
      ArticleViewDto articleViewDto = createArticleViewDto(articleViewHistory, requestUserId,
          article, 3, 5);

      given(articleService.view(articleId, requestUserId)).willReturn(articleViewDto);

      // when(실행), then(검증)
      mockMvc.perform(post("/api/articles/{articleId}/article-views", articleId)
              .header("Monew-Request-User-ID", requestUserId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(articleViewDto.id().toString()))
          .andExpect(jsonPath("$.viewedBy").value(articleViewDto.viewedBy().toString()))
          .andExpect(jsonPath("$.articleId").value(articleViewDto.articleId().toString()))
          .andExpect(jsonPath("$.source").value(articleViewDto.source().toString()));
    }

    @Test
    @DisplayName("존재하지 않거나 논리 삭제된 사용자 ID가 조회되면 404 상태코드와 UserNotFound 예외가 발생한다.")
    void fail_post_article_view_when_user_not_found() throws Exception {
      // given(준비)
      UUID requestUserId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();

      given(articleService.view(articleId, requestUserId)).willThrow(
          new UserNotFoundException(requestUserId));

      // when(실행), then(검증)
      mockMvc.perform(post("/api/articles/{articleId}/article-views", articleId)
              .header("Monew-Request-User-ID", requestUserId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.toString()))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.exceptionType").value(UserNotFoundException.class.getSimpleName()))
          .andExpect(jsonPath("$.details.userId").value(requestUserId.toString()));
    }

    @Test
    @DisplayName("존재하지 않거나 논리 삭제된 뉴스 기사 ID가 조회되면 404 상태코드와 ArticleNotFound 예외가 발생한다.")
    void fail_post_article_view_when_article_not_found() throws Exception {
      // given(준비)
      UUID requestUserId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();

      given(articleService.view(articleId, requestUserId)).willThrow(
          new ArticleNotFoundException(articleId));

      // when(실행), then(검증)
      mockMvc.perform(post("/api/articles/{articleId}/article-views", articleId)
              .header("Monew-Request-User-ID", requestUserId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value(ErrorCode.ARTICLE_NOT_FOUND.toString()))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(
              jsonPath("$.exceptionType").value(ArticleNotFoundException.class.getSimpleName()))
          .andExpect(jsonPath("$.details.articleId").value(articleId.toString()));
    }
  }
}