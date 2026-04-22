package com.codeit.monew.domain.comment.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.comment.dto.CommentCursorRequest;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.repository.CommentQueryRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.global.config.JpaAuditingConfig;
import com.codeit.monew.global.config.QueryDslConfig;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, QueryDslConfig.class, CommentQueryRepositoryImpl.class})
class CommentQueryRepositoryImplTest {

  @Autowired private TestEntityManager em;
  @Autowired private CommentQueryRepository commentQueryRepository;

  private User testUser;
  private Article testArticle;
  private List<Comment> comments = new ArrayList<>();

  @BeforeEach
  void setUp() throws InterruptedException {
    testUser = new User("test@test.com", "테스트 닉네임", "password1234!");
    em.persist(testUser);

    testArticle = Article.createArticle(ArticleSource.NAVER, "https://naver.com/3", "테스트 기사3", Instant.now(), "요약");
    em.persist(testArticle);

    // 댓글 5개 세팅
    for (int i = 1; i <= 5; i++) {
      Comment comment = new Comment(testArticle, testUser, "댓글 " + i);

      // 좋아요 수 세팅 - 1번: 10개, 2번: 10개(동점), 3번: 5개, 4번: 2개, 5번: 0개
      long likes = (i <= 2) ? 10L : (i == 3) ? 5L : (i == 4) ? 2L : 0L;
      ReflectionTestUtils.setField(comment, "likeCount", likes);

      em.persist(comment);
      Thread.sleep(10); // JpaAuditing의 createdAt이 확실하게 구분되도록 미세한 딜레이 부여
    }

    em.flush();
    em.clear();

    comments = em.getEntityManager()
        .createQuery("SELECT c FROM Comment c ORDER BY c.createdAt ASC", Comment.class)
        .getResultList();
  }

  @Nested
  @DisplayName("댓글 목록 조회 Query Repository 테스트")
  class read_comment {

    @Test
    @DisplayName("최초 조회 (cursor 없음) 시, 정해진 limit + 1개만큼 올바르게 조회된다.")
    void findCommentsByCursor_firstPage() {
      // given
      CommentCursorRequest request = new CommentCursorRequest(testArticle.getId(), "createdAt", "DESC", null, null, 3);

      // when
      List<Comment> result = commentQueryRepository.findCommentsByCursor(testArticle.getId(), request);

      // then
      assertThat(result).hasSize(4);
      // 최신순이므로 5번 댓글이 제일 먼저 나와야 함
      assertThat(result.get(0).getContent()).isEqualTo("댓글 5");
      assertThat(result.get(1).getContent()).isEqualTo("댓글 4");
    }

    @Test
    @DisplayName("createdAt 커서 조회 시, 이전 페이지의 마지막 작성일자 이후의 데이터를 조회한다.")
    void findCommentsByCursor_createdAt_desc() {
      // given
      Instant cursorAfter = comments.get(3).getCreatedAt(); // "댓글 4"의 정확한 DB 작성 시간
      CommentCursorRequest request = new CommentCursorRequest(testArticle.getId(), "createdAt", "DESC", null, cursorAfter, 2);

      // when
      List<Comment> result = commentQueryRepository.findCommentsByCursor(testArticle.getId(), request);

      // then
      assertThat(result).isNotEmpty();
      // 최신순(내림차순)에서 4번 댓글 다음은 더 과거에 작성된 3번 댓글이어야 함
      assertThat(result.get(0).getContent()).isEqualTo("댓글 3");
    }

    @Test
    @DisplayName("likeCount 복합 커서 조회 시, 좋아요 수가 같을 때 작성일자로 동점자를 가려낸다.")
    void findCommentsByCursor_likeCount_desc_tieBreaker() {
      // given
      String cursorLikeCount = "10";
      Instant cursorAfter = comments.get(1).getCreatedAt(); // "댓글 2"의 정확한 DB 작성 시간
      CommentCursorRequest request = new CommentCursorRequest(testArticle.getId(), "likeCount", "DESC", cursorLikeCount, cursorAfter, 3);

      // when
      List<Comment> result = commentQueryRepository.findCommentsByCursor(testArticle.getId(), request);

      // then
      // 2번 댓글(좋아요 10)의 다음 순위는
      // 1. 좋아요가 같으면서 더 과거에 쓴 1번 댓글 (좋아요 10)
      // 2. 그 다음으로 좋아요가 많은 3번 댓글 (좋아요 5)
      assertThat(result.get(0).getContent()).isEqualTo("댓글 1");
      assertThat(result.get(1).getContent()).isEqualTo("댓글 3");
    }

    @Test
    @DisplayName("좋아요순 조건문의 반대 분기(오름차순(ASC))도 정상 동작한다.")
    void findCommentsByCursor_likeCount_asc() {
      // given: 오름차순(ASC)으로 커서 요청 (3번 댓글 커서 기준)
      String cursorLikeCount = "5";
      Instant cursorAfter = comments.get(2).getCreatedAt();
      CommentCursorRequest request = new CommentCursorRequest(testArticle.getId(), "likeCount", "ASC", cursorLikeCount, cursorAfter, 2);

      // when
      List<Comment> result = commentQueryRepository.findCommentsByCursor(testArticle.getId(), request);

      // then
      // 좋아요 오름차순이므로, 5개짜리 다음은 10개짜리들이 나와야 함
      assertThat(result).isNotEmpty();
      assertThat(result.get(0).getLikeCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("createdAt순 조건문의 반대 분기(오름차순(ASC))도 정상 동작한다.")
    void findCommentsByCursor_createdAt_asc() {
      // given: 오름차순(ASC)으로 커서 요청 (3번 댓글 커서 기준)
      Instant cursorAfter = comments.get(2).getCreatedAt();
      CommentCursorRequest request = new CommentCursorRequest(testArticle.getId(), "createdAt", "ASC", null, cursorAfter, 2);

      // when
      List<Comment> result = commentQueryRepository.findCommentsByCursor(testArticle.getId(), request);

      // then
      assertThat(result).isNotEmpty();
      // 가장 오래된 것부터 오름차순이므로, 3번 다음은 더 최근에 쓰여진 4번 댓글이 나와야 함
      assertThat(result.get(0).getContent()).isEqualTo("댓글 4");
    }

    @Test
    @DisplayName("likeCount 정렬 시 커서 파라미터가 누락되면 조건 없이 무시된다.")
    void findCommentsByCursor_likeCount_missing_parameters() {
      // given
      CommentCursorRequest request = new CommentCursorRequest(testArticle.getId(), "likeCount", "DESC", null, null, 2);

      // when
      List<Comment> result = commentQueryRepository.findCommentsByCursor(testArticle.getId(), request);

      // then
      assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("createdAt 정렬 시 after 파라미터가 누락되면 조건 없이 무시된다.")
    void findCommentsByCursor_createdAt_missing_parameters() {
      // given
      CommentCursorRequest request = new CommentCursorRequest(testArticle.getId(), "createdAt", "DESC", "someCursor", null, 2);

      // when
      List<Comment> result = commentQueryRepository.findCommentsByCursor(testArticle.getId(), request);

      // then
      assertThat(result).hasSize(3);
    }
  }
}