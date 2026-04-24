package com.codeit.monew.domain.notification.listener;

import com.codeit.monew.domain.notification.event.BulkArticleRegisteredEvent;
import com.codeit.monew.domain.notification.event.CommentLikedEvent;
import com.codeit.monew.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {
  private final NotificationService notificationService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleBulkArticleScrapedEvent(BulkArticleRegisteredEvent event) {
    log.info("[NOTIFICATION_LISTENER] 비동기 기사 알림 처리 시작: 관심사 수={}", event.interestCounts().size());

    notificationService.createInterestNotifications(event);
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCommentLikedEvent(CommentLikedEvent event) {
    log.info("[NOTIFICATION_LISTENER] 비동기 댓글 좋아요 알림 처리 시작: commentId={}", event.commentId());

    notificationService.createCommentLikeNotification(
        event.commentId(),
        event.readerId(),
        event.likerNickname()
    );
  }
}
