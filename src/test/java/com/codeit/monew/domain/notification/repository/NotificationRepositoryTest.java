package com.codeit.monew.domain.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.monew.domain.notification.entity.Notification;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.config.JpaAuditingConfig;
import com.codeit.monew.global.config.QueryDslConfig;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, QueryDslConfig.class})
class NotificationRepositoryTest {
  @Autowired
  private NotificationRepository notificationRepository;

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
  @DisplayName("알림 Repository 벌크 연산 테스트")
  class ModifyingQueryTest {

    @Test
    @DisplayName("특정 유저의 모든 미확인 알림을 읽음 처리한다.")
    void confirmAllByUserId() {
      createTestNotification(testUser, "알림 1");
      createTestNotification(testUser, "알림 2");

      em.flush();
      em.clear();

      int updatedCount = notificationRepository.confirmAllByUserId(testUser.getId(), Instant.now());

      assertThat(updatedCount).isEqualTo(2);
    }

    @Test
    @DisplayName("주어진 시간 이전에 확인된 오래된 알림만 삭제한다.")
    void deleteOldConfirmedNotifications() {
      Instant eightDaysAgo = Instant.now().minus(8, ChronoUnit.DAYS);
      Notification oldConfirmedNoti = createTestNotification(testUser, "오래된 알림");
      ReflectionTestUtils.setField(oldConfirmedNoti, "confirmedAt", eightDaysAgo);

      em.flush();
      em.clear();

      int deletedCount = notificationRepository.deleteOldConfirmedNotifications(Instant.now().minus(7, ChronoUnit.DAYS));

      assertThat(deletedCount).isEqualTo(1);
    }
  }
}