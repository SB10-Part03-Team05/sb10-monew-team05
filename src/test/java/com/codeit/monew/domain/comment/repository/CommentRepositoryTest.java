package com.codeit.monew.domain.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.global.config.JpaAuditingConfig;
import com.codeit.monew.global.config.QueryDslConfig;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, QueryDslConfig.class})
class CommentRepositoryTest {

  @Autowired private TestEntityManager em;
  @Autowired private CommentRepository commentRepository;

  private User testUser;
  private Article testArticle;
  private Comment testComment;

  @BeforeEach
  void setUp() {
    testUser = new User("test@test.com", "테스터", "password");
    em.persist(testUser);

    testArticle = Article.createArticle(ArticleSource.NAVER, "https://naver.com/1", "테스트 기사", Instant.now(), "요약");
    em.persist(testArticle);

    testComment = new Comment(testArticle, testUser, "테스트 댓글입니다.");
    em.persist(testComment);

    em.flush();
    em.clear();
  }

  @Test
  @DisplayName("countByArticleIdAndDeletedAtIsNull: 삭제되지 않은 기사의 댓글 개수를 정확히 샌다.")
  void countByArticleIdAndDeletedAtIsNull() {
    // given
    Comment deletedComment = new Comment(testArticle, testUser, "삭제될 댓글");
    em.persist(deletedComment);

    commentRepository.delete(deletedComment);
    em.flush();
    em.clear();

    // when
    long count = commentRepository.countByArticleIdAndDeletedAtIsNull(testArticle.getId());

    // then
    assertThat(count).isEqualTo(1L);
  }

  @Test
  @DisplayName("findByIdWithUser: fetch join을 통해 댓글과 유저 정보를 한 번에 가져온다.")
  void findByIdWithUser() {
    // when
    Optional<Comment> foundComment = commentRepository.findByIdWithUser(testComment.getId());

    // then
    assertThat(foundComment).isPresent();
    assertThat(foundComment.get().getUser().getNickname()).isEqualTo("테스터");
  }

  @Test
  @DisplayName("deleteByIdHard: @Modifying 쿼리가 실행되어 DB에서 댓글이 완전히 삭제된다.")
  void deleteByIdHard() {
    // when
    commentRepository.deleteByIdHard(testComment.getId());
    em.clear();

    // then
    Optional<Comment> deletedComment = commentRepository.findById(testComment.getId());
    assertThat(deletedComment).isEmpty();
  }
}