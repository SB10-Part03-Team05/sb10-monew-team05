package com.codeit.monew.domain.notification.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.notification.entity.CommentNotification;
import com.codeit.monew.domain.notification.entity.Notification;
import com.codeit.monew.domain.notification.repository.impl.NotificationQueryRepositoryImpl;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.config.JpaAuditingConfig;
import com.codeit.monew.global.config.QueryDslConfig;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, QueryDslConfig.class, NotificationQueryRepositoryImpl.class})
class NotificationQueryRepositoryImplTest {
  @Autowired
  private NotificationQueryRepositoryImpl notificationQueryRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TestEntityManager em;

  private User testUser;

  private User createTestUser(String email, String nickname) {
    User user = new User(email, nickname, "Password1234!");
    return userRepository.save(user);
  }

  private Article createTestArticle() {
    String uniqueUrl = "https://news.com/" + UUID.randomUUID().toString();

    ArticleSource dummySource = ArticleSource.values()[0];

    Article article = Article.createArticle(
        dummySource,
        uniqueUrl,
        "테스트 기사 제목",
        Instant.now(),
        "테스트 기사 요약"
    );
    return em.persist(article);
  }

  private Comment createTestComment(User user) {
    Article article = createTestArticle();
    Comment comment = new Comment(article, user, "테스트 댓글");
    return em.persist(comment);
  }

  private Notification createTestNotification(User user, String content, Instant createdAt) {
    Comment comment = createTestComment(user);
    CommentNotification notification = CommentNotification.create(user, content, comment);

    em.persist(notification);
    em.flush();

    em.getEntityManager()
        .createQuery("UPDATE Notification n SET n.createdAt = :createdAt WHERE n.id = :id")
        .setParameter("createdAt", createdAt)
        .setParameter("id", notification.getId())
        .executeUpdate();

    em.clear();

    return em.find(CommentNotification.class, notification.getId());
  }

  @BeforeEach
  void setUp() {
    testUser = createTestUser("test@test.com", "tester");
  }

  @Nested
  @DisplayName("알림 Repository QueryDSL 커서 페이징 테스트")
  class CursorPagingTest {

    @Test
    @DisplayName("미확인 알림 개수를 정확히 카운트한다.")
    void countUnconfirmedByUserId() {
      Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
      Instant t2 = Instant.parse("2026-01-01T00:00:01Z");
      Notification noti1 = createTestNotification(testUser, "알림 1", t1);
      Notification noti2 = createTestNotification(testUser, "알림 2", t2);

      long count = notificationQueryRepository.countUnconfirmedByUserId(testUser.getId());

      assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("커서 기반 페이징 조회가 정상 작동한다.")
    void findUnconfirmedByCursor() {
      Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
      Instant t2 = Instant.parse("2026-01-01T00:00:01Z");
      Notification noti1 = createTestNotification(testUser, "알림 1", t1);
      Notification noti2 = createTestNotification(testUser, "알림 2", t2);

      List<Notification> result = notificationQueryRepository.findUnconfirmedByCursor(
          testUser.getId(), null, null, 10
      );

      assertThat(result).hasSize(2);
      assertThat(result.get(0).getId()).isEqualTo(noti2.getId()); // 최신순 정렬
    }
  }
}