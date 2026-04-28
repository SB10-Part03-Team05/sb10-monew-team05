package com.codeit.monew.domain.notification.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.notification.entity.Notification;
import com.codeit.monew.domain.notification.repository.impl.NotificationQueryRepositoryImpl;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.config.JpaAuditingConfig;
import com.codeit.monew.global.config.QueryDslConfig;
import java.util.List;
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

  private Notification createTestNotification(User user, String content) {
    try {
      Class<?> clazz = Class.forName("com.codeit.monew.domain.notification.entity.CommentNotification");
      java.lang.reflect.Constructor<?> constructor = clazz.getDeclaredConstructor();
      constructor.setAccessible(true);
      Notification notification = (Notification) constructor.newInstance();

      org.springframework.test.util.ReflectionTestUtils.setField(notification, "user", user);
      org.springframework.test.util.ReflectionTestUtils.setField(notification, "content", content);

      org.springframework.test.util.ReflectionTestUtils.setField(notification, "resourceId", java.util.UUID.randomUUID());

      return em.persist(notification);
    } catch (Exception e) {
      throw new RuntimeException("테스트 알림 생성 실패", e);
    }
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
      createTestNotification(testUser, "알림 1");
      createTestNotification(testUser, "알림 2");

      long count = notificationQueryRepository.countUnconfirmedByUserId(testUser.getId());

      assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("커서 기반 페이징 조회가 정상 작동한다.")
    void findUnconfirmedByCursor() throws InterruptedException {
      Notification noti1 = createTestNotification(testUser, "알림 1");
      Thread.sleep(10);
      Notification noti2 = createTestNotification(testUser, "알림 2");

      List<Notification> result = notificationQueryRepository.findUnconfirmedByCursor(
          testUser.getId(), null, null, 10
      );

      assertThat(result).hasSize(2);
      assertThat(result.get(0).getId()).isEqualTo(noti2.getId()); // 최신순 정렬
    }
  }
}