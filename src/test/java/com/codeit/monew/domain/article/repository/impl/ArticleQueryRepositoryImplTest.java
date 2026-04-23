package com.codeit.monew.domain.article.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.codeit.monew.domain.article.dto.response.CursorPageResponseArticleDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleInterest;
import com.codeit.monew.domain.article.entity.ArticleViewHistory;
import com.codeit.monew.domain.article.entity.type.ArticleDirection;
import com.codeit.monew.domain.article.entity.type.ArticleOrderBy;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.article.repository.ArticleViewHistoryRepository;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.config.JpaAuditingConfig;
import com.codeit.monew.global.config.QueryDslConfig;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, QueryDslConfig.class})
public class ArticleQueryRepositoryImplTest {

  @Autowired
  private ArticleRepository articleRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private InterestRepository interestRepository;

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private ArticleViewHistoryRepository articleViewHistoryRepository;

  @Autowired
  private ArticleQueryRepositoryImpl articleQueryRepository;

  @Autowired
  private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    articleQueryRepository = new ArticleQueryRepositoryImpl(new JPAQueryFactory(entityManager));
  }

  private ArticleSearchRequest createRequest(UUID interestId, ArticleOrderBy orderBy,
      ArticleDirection direction, int limit) {

    ArticleSearchRequest request = new ArticleSearchRequest();
    request.setInterestId(interestId);
    request.setOrderBy(orderBy);
    request.setDirection(direction);
    request.setLimit(limit);
    return request;
  }

  private Article createArticle(ArticleSource source, String sourceUrl,
      String title, Instant publishDate, String summary) {

    Article article = Article.createArticle(source, sourceUrl, title, publishDate, summary);
    return articleRepository.save(article);
  }

  private User createUser(String email, String nickname, String password) {
    User user = new User(email, nickname, password);
    return userRepository.save(user);
  }

  private Interest createInterest(String name) {
    Interest interest = Interest.create(name);
    return interestRepository.save(interest);
  }

  private void createArticleInterest(Article article, Interest interest) {
    entityManager.persist(ArticleInterest.create(article, interest));
  }

  private Comment createComment(Article article, User user, String content) {
    Comment comment = new Comment(article, user, content);
    return commentRepository.save(comment);
  }

  private ArticleViewHistory createViewHistory(User user, Article article) {
    ArticleViewHistory viewHistory = BeanUtils.instantiateClass(ArticleViewHistory.class);
    ReflectionTestUtils.setField(viewHistory, "user", user);
    ReflectionTestUtils.setField(viewHistory, "article", article);
    return articleViewHistoryRepository.save(viewHistory);
  }

  @Nested
  @DisplayName("뉴스 기사 목록 조회 Repository 테스트")
  class search {

    @Test
    @DisplayName("publishDate로 정렬된 논리 삭제되지 않은 저장된 뉴스 기사 목록을 조회할 수 있다.")
    void search_with_publishDate() {
      // given
      User requestUser = createUser("request@test.com", "request", "password123!");
      Interest interest = createInterest("삼성");

      Article article1 = createArticle(ArticleSource.NAVER, "https://news/1", "기사1",
          Instant.parse("2026-04-20T10:00:00Z"), "기사1 요약");
      Article article2 = createArticle(ArticleSource.NAVER, "https://news/2", "기사2",
          Instant.parse("2026-04-19T10:00:00Z"), "기사2 요약");
      Article article3 = createArticle(ArticleSource.NAVER, "https://news/3", "기사3",
          Instant.parse("2026-04-18T10:00:00Z"), "기사3 요약");

      createArticleInterest(article1, interest);
      createArticleInterest(article2, interest);
      createArticleInterest(article3, interest);

      entityManager.flush();
      entityManager.clear();

      ArticleSearchRequest request = createRequest(interest.getId(), ArticleOrderBy.publishDate,
          ArticleDirection.DESC, 2);

      // when
      CursorPageResponseArticleDto result = articleQueryRepository.searchArticleList(request,
          requestUser.getId());

      // then
      assertThat(result.content()).hasSize(2);
      assertThat(result.content().get(0).id()).isEqualTo(article1.getId());
      assertThat(result.content().get(1).id()).isEqualTo(article2.getId());
      assertThat(result.content().get(0).publishDate()).isAfter(
          result.content().get(1).publishDate());

      assertThat(result.hasNext()).isTrue();
      assertThat(result.nextAfter()).isNotNull();
      assertThat(result.totalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("commentCount로 정렬된 논리 삭제되지 않은 저장된 뉴스 기사 목록을 조회할 수 있다.")
    void search_with_commentCount() {
      // given
      User requestUser = createUser("request@test.com", "request", "password123!");
      User user1 = createUser("u1@test.com", "u1", "password123!");
      User user2 = createUser("u2@test.com", "u2", "password123!");
      User user3 = createUser("u3@test.com", "u3", "password123!");
      User user4 = createUser("u4@test.com", "u4", "password123!");
      User user5 = createUser("u5@test.com", "u5", "password123!");
      User user6 = createUser("u6@test.com", "u6", "password123!");

      Interest interest = createInterest("삼성");

      Article article1 = createArticle(ArticleSource.NAVER, "https://news/comment-1", "댓글 많은 기사",
          Instant.parse("2026-04-20T10:00:00Z"), "요약1");
      Article article2 = createArticle(ArticleSource.NAVER, "https://news/comment-2", "댓글 중간 기사",
          Instant.parse("2026-04-19T10:00:00Z"), "요약2");
      Article article3 = createArticle(ArticleSource.NAVER, "https://news/comment-3", "댓글 적은 기사",
          Instant.parse("2026-04-18T10:00:00Z"), "요약3");

      createArticleInterest(article1, interest);
      createArticleInterest(article2, interest);
      createArticleInterest(article3, interest);

      createComment(article1, user1, "a1");
      createComment(article1, user2, "a2");
      createComment(article1, user3, "a3");

      createComment(article2, user4, "b1");
      createComment(article2, user5, "b2");

      createComment(article3, user6, "c1");

      entityManager.flush();
      entityManager.clear();

      ArticleSearchRequest request = createRequest(interest.getId(), ArticleOrderBy.commentCount,
          ArticleDirection.DESC, 2);

      // when
      CursorPageResponseArticleDto result = articleQueryRepository.searchArticleList(request,
          requestUser.getId());

      // then
      assertThat(result.content()).hasSize(2);
      assertThat(result.content().get(0).id()).isEqualTo(article1.getId());
      assertThat(result.content().get(1).id()).isEqualTo(article2.getId());
      assertThat(result.content().get(0).commentCount()).isEqualTo(3);
      assertThat(result.content().get(1).commentCount()).isEqualTo(2);

      assertThat(result.hasNext()).isTrue();
      assertThat(result.nextAfter()).isNotNull();
      assertThat(result.totalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("viewCount로 정렬된 논리 삭제되지 않은 저장된 뉴스 기사 목록을 조회할 수 있다.")
    void search_with_viewCount() {
      // given
      User requestUser = createUser("request@test.com", "request", "password123!");
      User user1 = createUser("u1@test.com", "u1", "password123!");
      User user2 = createUser("u2@test.com", "u2", "password123!");
      User user3 = createUser("u3@test.com", "u3", "password123!");
      User user4 = createUser("u4@test.com", "u4", "password123!");
      User user5 = createUser("u5@test.com", "u5", "password123!");
      User user6 = createUser("u6@test.com", "u6", "password123!");

      Interest interest = createInterest("삼성");

      Article article1 = createArticle(ArticleSource.NAVER, "https://news/view-1", "조회 많은 기사",
          Instant.parse("2026-04-20T10:00:00Z"), "요약1");
      Article article2 = createArticle(ArticleSource.NAVER, "https://news/view-2", "조회 중간 기사",
          Instant.parse("2026-04-19T10:00:00Z"), "요약2");
      Article article3 = createArticle(ArticleSource.NAVER, "https://news/view-3", "조회 적은 기사",
          Instant.parse("2026-04-18T10:00:00Z"), "요약3");

      createArticleInterest(article1, interest);
      createArticleInterest(article2, interest);
      createArticleInterest(article3, interest);

      createViewHistory(user1, article1);
      createViewHistory(user2, article1);
      createViewHistory(user3, article1);

      createViewHistory(user4, article2);
      createViewHistory(user5, article2);

      createViewHistory(user6, article3);

      entityManager.flush();
      entityManager.clear();

      ArticleSearchRequest request = createRequest(interest.getId(), ArticleOrderBy.viewCount,
          ArticleDirection.DESC, 2);

      // when
      CursorPageResponseArticleDto result = articleQueryRepository.searchArticleList(request,
          requestUser.getId());

      // then
      assertThat(result.content()).hasSize(2);
      assertThat(result.content().get(0).id()).isEqualTo(article1.getId());
      assertThat(result.content().get(1).id()).isEqualTo(article2.getId());
      assertThat(result.content().get(0).viewCount()).isEqualTo(3);
      assertThat(result.content().get(1).viewCount()).isEqualTo(2);

      assertThat(result.hasNext()).isTrue();
      assertThat(result.nextAfter()).isNotNull();
      assertThat(result.totalElements()).isEqualTo(3);
    }
  }
}
