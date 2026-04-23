package com.codeit.monew.domain.notification.service;

import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.interest.entity.Subscription;
import com.codeit.monew.domain.interest.repository.SubscriptionRepository;
import com.codeit.monew.domain.notification.entity.CommentNotification;
import com.codeit.monew.domain.notification.entity.InterestNotification;
import com.codeit.monew.domain.notification.event.BulkArticleRegisteredEvent;
import com.codeit.monew.domain.notification.repository.NotificationRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.exception.comment.CommentNotFoundException;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final UserRepository userRepository;
  private final CommentRepository commentRepository;

  // 기사 등록 알림
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void createInterestNotifications(BulkArticleRegisteredEvent event) {
    if (event.interestCounts() == null || event.interestCounts().isEmpty()) return;

    // 1. 이벤트로 넘어온 관심사 ID 목록 추출
    List<UUID> targetInterestIds = event.interestCounts().stream()
        .map(BulkArticleRegisteredEvent.InterestArticleCount::interestId)
        .toList();

    // 2. 해당 관심사들의 구독 정보(유저, 관심사)를 한 번에 조회
    List<Subscription> subscriptions = subscriptionRepository.findAllByInterestIdInWithUserAndInterest(targetInterestIds);
    if (subscriptions.isEmpty()) return;

    // 3. 구독자 리스트 - 관심사 ID를 key로 하는 Map으로 그룹핑
    Map<UUID, List<Subscription>> subscriptionsByInterestId = subscriptions.stream()
        .collect(Collectors.groupingBy(sub -> sub.getInterest().getId()));

    List<InterestNotification> notificationsToSave = new ArrayList<>();

    // 4. 알림 조립
    for (BulkArticleRegisteredEvent.InterestArticleCount countInfo : event.interestCounts()) {

      // 해당 관심사를 구독하는 목록 꺼내기 (없으면 빈 리스트 반환하여 NullPointerException 방어)
      List<Subscription> matchedSubscriptions = subscriptionsByInterestId.getOrDefault(countInfo.interestId(), List.of());
      if (matchedSubscriptions.isEmpty()) continue;

      // 알림 문구 생성
      String content = String.format("[%s]와 관련된 기사가 %d건 등록되었습니다.", countInfo.interestName(), countInfo.articleCount());

      // 구독자 수만큼 엔티티 생성
      for (Subscription sub : matchedSubscriptions) {
        notificationsToSave.add(
            InterestNotification.create(sub.getUser(), content, sub.getInterest())
        );
      }
    }

    // 5. 일괄 저장
    if (!notificationsToSave.isEmpty()) {
      notificationRepository.saveAll(notificationsToSave);
      log.info("[NOTIFICATION_SERVICE] 관심사 기사 알림 대량 생성 완료: 총 {}건", notificationsToSave.size());
    }
  }

  // 댓글 좋아요 알림
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void createCommentLikeNotification(UUID commentId, UUID readerId, String likerNickname) {
    // 1. 알림을 받을 사람 (댓글 작성자) 조회
    User reader = userRepository.findByIdAndDeletedAtIsNull(readerId)
        .orElseThrow(() -> {
          log.warn("[NOTIFICATION_SERVICE] 댓글 좋아요 알림 실패 - 유저 없음: readerId={}", readerId);
          return new UserNotFoundException(readerId);
        });

    // 2. 대상 댓글 조회
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> {
          log.warn("[NOTIFICATION_SERVICE] 댓글 좋아요 알림 실패 - 댓글 없음: commentId={}", commentId);
          return new CommentNotFoundException(commentId);
        });

    // 2-1. 이벤트로 넘어온 readerId가 실제 댓글 작성자가 맞는지 교차 검증
    if (comment.getUser() == null || !comment.getUser().getId().equals(reader.getId())) {
      log.warn("[NOTIFICATION_SERVICE] 댓글 좋아요 알림 실패 - 수신자 불일치: commentId={}, readerId={}, commentOwnerId={}",
          commentId, readerId, comment.getUser() == null ? null : comment.getUser().getId());
      throw new IllegalStateException("댓글 작성자와 알림을 받을 사용자의 ID값이 일치하지 않습니다.");
    }

    // 3. 알림 내용 생성
    String content = String.format("[%s]님이 나의 댓글을 좋아합니다.", likerNickname);
    CommentNotification notification = CommentNotification.create(reader, content, comment);

    // 4. 저장
    notificationRepository.save(notification);
    log.info("[NOTIFICATION_SERVICE] 댓글 좋아요 알림 생성 완료: notificationId={}", notification.getId());
  }
}
