package com.codeit.monew.domain.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.entity.CommentLike;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.global.config.JpaAuditingConfig;
import com.codeit.monew.global.config.QueryDslConfig;
import java.time.Instant;
import java.util.List;
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
class CommentLikeRepositoryTest {

  @Autowired private TestEntityManager em;
  @Autowired private CommentLikeRepository commentLikeRepository;

  private User testUser;
  private Article testArticle;
  private Comment testComment1;
  private Comment testComment2;

  @BeforeEach
  void setUp() {
    testUser = new User("test@test.com", "테스터", "password");
    em.persist(testUser);

    testArticle = Article.createArticle(ArticleSource.NAVER, "https://naver.com/2", "테스트 기사2", Instant.now(), "요약");
    em.persist(testArticle);

    testComment1 = new Comment(testArticle, testUser, "첫 번째 댓글");
    testComment2 = new Comment(testArticle, testUser, "두 번째 댓글");
    em.persist(testComment1);
    em.persist(testComment2);

    CommentLike commentLike = new CommentLike(testComment1, testUser);
    em.persist(commentLike);

    em.flush();
    em.clear();
  }

  @Test
  @DisplayName("existsByCommentIdAndUserId: 좋아요 존재 여부를 정확히 반환한다.")
  void existsByCommentIdAndUserId() {
    // when
    boolean exists1 = commentLikeRepository.existsByCommentIdAndUserId(testComment1.getId(), testUser.getId());
    boolean exists2 = commentLikeRepository.existsByCommentIdAndUserId(testComment2.getId(), testUser.getId());

    // then
    assertThat(exists1).isTrue();
    assertThat(exists2).isFalse();
  }

  @Test
  @DisplayName("deleteByCommentIdAndUserId: 특정 유저의 특정 댓글 좋아요만 삭제하고 1을 반환한다.")
  void deleteByCommentIdAndUserId() {
    // when
    int deletedCount = commentLikeRepository.deleteByCommentIdAndUserId(testComment1.getId(), testUser.getId());
    em.clear();

    // then
    assertThat(deletedCount).isEqualTo(1);
    boolean exists = commentLikeRepository.existsByCommentIdAndUserId(testComment1.getId(), testUser.getId());
    assertThat(exists).isFalse();
  }

  @Test
  @DisplayName("deleteByCommentId: 댓글 물리 삭제 시 해당 댓글에 달린 모든 좋아요가 삭제된다.")
  void deleteByCommentId() {
    // given
    User anotherUser = new User("other@test.com", "다른유저", "password");
    em.persist(anotherUser);
    CommentLike anotherLike = new CommentLike(testComment1, anotherUser);
    em.persist(anotherLike);
    em.flush();
    em.clear();

    // when
    commentLikeRepository.deleteByCommentId(testComment1.getId());
    em.clear();

    // then
    assertThat(commentLikeRepository.existsByCommentIdAndUserId(testComment1.getId(), testUser.getId())).isFalse();
    assertThat(commentLikeRepository.existsByCommentIdAndUserId(testComment1.getId(), anotherUser.getId())).isFalse();
  }

  @Test
  @DisplayName("findLikedCommentIdsByUserAndComments: N+1 방어 IN 쿼리가 정확히 동작한다.")
  void findLikedCommentIdsByUserAndComments() {
    // given
    List<java.util.UUID> searchIds = List.of(testComment1.getId(), testComment2.getId());

    // when
    List<java.util.UUID> likedIds = commentLikeRepository.findLikedCommentIdsByUserAndComments(testUser.getId(), searchIds);

    // then
    assertThat(likedIds).hasSize(1);
    assertThat(likedIds).containsExactly(testComment1.getId());
  }
}
