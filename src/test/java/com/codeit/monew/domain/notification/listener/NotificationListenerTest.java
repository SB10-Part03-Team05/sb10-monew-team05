package com.codeit.monew.domain.notification.listener;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.notification.event.BulkArticleRegisteredEvent;
import com.codeit.monew.domain.notification.event.CommentLikedEvent;
import com.codeit.monew.domain.notification.service.NotificationService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {
  @InjectMocks
  private NotificationListener notificationListener;

  @Mock
  private NotificationService notificationService;

  @Test
  @DisplayName("BulkArticleRegisteredEvent 수신 시 서비스의 기사 알림 생성 메서드를 호출한다.")
  void handleBulkArticleRegisteredEvent() {
    // given
    BulkArticleRegisteredEvent event = new BulkArticleRegisteredEvent(
        List.of(new BulkArticleRegisteredEvent.InterestArticleCount(UUID.randomUUID(), "테스트", 5))
    );

    // when
    notificationListener.handleBulkArticleScrapedEvent(event);

    // then
    verify(notificationService, times(1)).createInterestNotifications(event);
  }

  @Test
  @DisplayName("CommentLikedEvent 수신 시 서비스의 댓글 좋아요 알림 생성 메서드를 호출한다.")
  void handleCommentLikedEvent() {
    // given
    UUID commentId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();
    UUID readerId = UUID.randomUUID();
    String likerNickname = "tester";
    CommentLikedEvent event = new CommentLikedEvent(commentId, articleId, readerId, likerNickname);

    // when
    notificationListener.handleCommentLikedEvent(event);

    // then
    verify(notificationService, times(1)).createCommentLikeNotification(commentId, readerId, likerNickname);
  }
}