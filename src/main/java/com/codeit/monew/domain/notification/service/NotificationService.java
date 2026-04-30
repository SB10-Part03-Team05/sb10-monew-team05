package com.codeit.monew.domain.notification.service;

import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.interest.entity.Subscription;
import com.codeit.monew.domain.interest.repository.SubscriptionRepository;
import com.codeit.monew.domain.notification.dto.NotificationDto;
import com.codeit.monew.domain.notification.dto.NotificationListDto;
import com.codeit.monew.domain.notification.entity.CommentNotification;
import com.codeit.monew.domain.notification.entity.InterestNotification;
import com.codeit.monew.domain.notification.entity.Notification;
import com.codeit.monew.domain.notification.event.BulkArticleRegisteredEvent;
import com.codeit.monew.domain.notification.mapper.NotificationMapper;
import com.codeit.monew.domain.notification.repository.NotificationQueryRepository;
import com.codeit.monew.domain.notification.repository.NotificationRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.exception.comment.CommentNotFoundException;
import com.codeit.monew.global.exception.notification.NotificationAccessDeniedException;
import com.codeit.monew.global.exception.notification.NotificationNotFoundException;
import com.codeit.monew.global.exception.notification.NotificationReceiverMismatchException;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
  private final NotificationQueryRepository notificationQueryRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final UserRepository userRepository;
  private final CommentRepository commentRepository;
  private final NotificationMapper notificationMapper;

  // 기사 등록 알림
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void createInterestNotifications(BulkArticleRegisteredEvent event) {
    if (event == null || event.interestCounts() == null || event.interestCounts().isEmpty()) return;

    // 이벤트 페이로드에 중복된 관심사 ID가 있더라도 개수를 합산하여 멱등성 보장
    Map<UUID, BulkArticleRegisteredEvent.InterestArticleCount> mergedCountsMap = event.interestCounts().stream()
        .collect(Collectors.toMap(
            BulkArticleRegisteredEvent.InterestArticleCount::interestId,
            countInfo -> countInfo,
            // 중복된 ID가 발견되면, 이전 값과 현재 값의 기사 개수를 더해 새로운 객체 생성
            (prev, curr) -> new BulkArticleRegisteredEvent.InterestArticleCount(
                prev.interestId(),
                prev.interestName(),
                prev.articleCount() + curr.articleCount()
            ),
            LinkedHashMap::new // 순서 보장
        ));

    // 1. 이벤트로 넘어온 관심사 ID 목록 추출
    List<UUID> targetInterestIds = new ArrayList<>(mergedCountsMap.keySet());

    // 2. 해당 관심사들의 구독 정보(유저, 관심사)를 한 번에 조회
    List<Subscription> subscriptions = subscriptionRepository.findAllByInterestIdInWithUserAndInterest(targetInterestIds);
    if (subscriptions.isEmpty()) return;

    // 3. 구독자 리스트 - 관심사 ID를 key로 하는 Map으로 그룹핑
    Map<UUID, List<Subscription>> subscriptionsByInterestId = subscriptions.stream()
        .collect(Collectors.groupingBy(sub -> sub.getInterest().getId()));

    List<InterestNotification> notificationsToSave = new ArrayList<>();

    // 4. 알림 조립
    for (BulkArticleRegisteredEvent.InterestArticleCount countInfo : mergedCountsMap.values()) {

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
      log.info("[NOTIFICATION_CREATE] 관심사 기사 알림 대량 생성 완료: 총 {}건", notificationsToSave.size());
    }
  }

  // 댓글 좋아요 알림
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void createCommentLikeNotification(UUID commentId, UUID readerId, String likerNickname) {
    // 1. 알림을 받을 사람 (댓글 작성자) 조회
    User reader = userRepository.findByIdAndDeletedAtIsNull(readerId)
        .orElseThrow(() -> {
          log.warn("[NOTIFICATION_CREATE] 댓글 좋아요 알림 실패 - 유저 없음: readerId={}", readerId);
          return new UserNotFoundException(readerId);
        });

    // 2. 대상 댓글 조회
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> {
          log.warn("[NOTIFICATION_CREATE] 댓글 좋아요 알림 실패 - 댓글 없음: commentId={}", commentId);
          return new CommentNotFoundException(commentId);
        });

    // 2-1. 이벤트로 넘어온 readerId가 실제 댓글 작성자가 맞는지 교차 검증
    if (comment.getUser() == null || !comment.getUser().getId().equals(reader.getId())) {
      log.warn("[NOTIFICATION_CREATE] 댓글 좋아요 알림 실패 - 수신자 불일치: commentId={}, readerId={}, commentOwnerId={}",
          commentId, readerId, comment.getUser() == null ? null : comment.getUser().getId());
      throw new NotificationReceiverMismatchException(commentId, readerId);
    }

    // 3. 알림 내용 생성
    String content = String.format("[%s]님이 나의 댓글을 좋아합니다.", likerNickname);
    CommentNotification notification = CommentNotification.create(reader, content, comment);

    // 4. 저장
    notificationRepository.save(notification);
    log.info("[NOTIFICATION_CREATE] 댓글 좋아요 알림 생성 완료: notificationId={}", notification.getId());
  }

  // 단건 알림 확인
  @Transactional
  public void confirmNotification(UUID notificationId, UUID userId) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> {
          log.warn("[NOTIFICATION_CONFIRM] 단건 확인 실패 - 알림 없음: notificationId={}", notificationId);
          return new NotificationNotFoundException(notificationId);
        });

    // 본인의 알림만 확인 가능
    if (!notification.getUser().getId().equals(userId)) {
      log.warn("[NOTIFICATION_CONFIRM] 단건 확인 실패 - 권한 없음: notificationId={}, requestUserId={}", notificationId, userId);
      throw new NotificationAccessDeniedException(notificationId, userId);
    }

    notification.confirm(); // 더티 체킹으로 confirmed = true 변경
  }

  // 전체 알림 확인
  @Transactional
  public void confirmAllNotifications(UUID userId) {
    int updatedCount = notificationRepository.confirmAllByUserId(userId, Instant.now());
    log.info("[NOTIFICATION_CONFIRM] 유저 {}의 알림 {}건 전체 읽음 처리 완료", userId, updatedCount);
  }

  // 안 읽은 알림 목록 조회
  @Transactional(readOnly = true)
  public NotificationListDto getUnconfirmedNotifications(UUID userId, Instant after, UUID cursor, int limit) {
    log.info("[NOTIFICATION_READ] 안 읽은 알림 목록 조회 요청: userId={}, after={}, cursor={}, limit={}", userId, after, cursor, limit);

    // 1. 커서 기반 데이터 조회
    List<Notification> notifications = notificationQueryRepository.findUnconfirmedByCursor(userId, after, cursor, limit);

    // 2. 다음 페이지 존재 여부 판단 및 초과 데이터 제거
    boolean hasNext = notifications.size() > limit;
    if (hasNext) {
      notifications.remove(limit);
    }

    // 3. DTO 변환
    List<NotificationDto> content = notifications.stream()
        .map(notificationMapper::toDto)
        .toList();

    // 4. 다음 요청을 위한 커서 값 계산
    String nextCursor = null;
    Instant nextAfter = null;
    if (!content.isEmpty()) {
      // 조회된 데이터가 있다면 리스트의 가장 마지막 요소의 값들을 다음 커서로 지정
      NotificationDto lastItem = content.get(content.size() - 1);
      nextCursor = lastItem.id().toString();
      nextAfter = lastItem.createdAt();
    }

    // 5. 전체 안 읽은 알림 개수 조회
    long totalElements = notificationQueryRepository.countUnconfirmedByUserId(userId);

    log.info("[NOTIFICATION_READ] 알림 목록 조회 완료: 반환된 데이터 수={}, 남은 전체 안 읽은 개수={}, hasNext={}", content.size(), totalElements, hasNext);

    // 6. 최종 응답 DTO 생성
    return new NotificationListDto(
        content,
        nextCursor,
        nextAfter,
        content.size(),
        totalElements,
        hasNext);
  }

  // 확인한 알림 중 7일이 경과된 알림 삭제
  @Transactional
  public int cleanUpOldNotifications() {
    Instant targetTime = Instant.now().minus(7, ChronoUnit.DAYS);

    int deletedCount = notificationRepository.deleteOldConfirmedNotifications(targetTime);
    log.info("[NOTIFICATION_DELETE] 7일 경과된 읽은 알림 삭제 완료: {}건", deletedCount);
    return deletedCount;
  }
}
