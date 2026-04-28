package com.codeit.monew.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.comment.entity.Comment;
import com.codeit.monew.domain.comment.repository.CommentRepository;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.notification.entity.CommentNotification;
import com.codeit.monew.domain.notification.entity.InterestNotification;
import com.codeit.monew.domain.notification.entity.Notification;
import com.codeit.monew.domain.notification.event.BulkArticleRegisteredEvent;
import com.codeit.monew.domain.notification.mapper.NotificationMapper;
import com.codeit.monew.domain.notification.repository.NotificationQueryRepository;
import com.codeit.monew.domain.notification.repository.NotificationRepository;
import com.codeit.monew.domain.interest.entity.Subscription;
import com.codeit.monew.domain.interest.repository.SubscriptionRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.exception.comment.CommentNotFoundException;
import com.codeit.monew.global.exception.notification.NotificationAccessDeniedException;
import com.codeit.monew.global.exception.notification.NotificationNotFoundException;
import com.codeit.monew.global.exception.notification.NotificationReceiverMismatchException;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @InjectMocks
  private NotificationService notificationService;

  @Mock
  private NotificationRepository notificationRepository;
  @Mock
  private SubscriptionRepository subscriptionRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private CommentRepository commentRepository;
  @Mock
  private NotificationQueryRepository notificationQueryRepository;
  @Mock
  private NotificationMapper notificationMapper;

  @Captor
  private ArgumentCaptor<List<InterestNotification>> interestNotificationListCaptor;
  @Captor
  private ArgumentCaptor<CommentNotification> commentNotificationCaptor;

  @Nested
  @DisplayName("기사 등록 알림 테스트")
  class CreateInterestNotificationsTest {

    @Test
    @DisplayName("이벤트 데이터가 null이거나 비어있으면 조기 종료된다.")
    void fail_create_notification_event_is_null_or_empty() {
      // given
      BulkArticleRegisteredEvent emptyEvent = new BulkArticleRegisteredEvent(
          Collections.emptyList());
      BulkArticleRegisteredEvent nullEvent = new BulkArticleRegisteredEvent(null);

      // when
      notificationService.createInterestNotifications(emptyEvent);
      notificationService.createInterestNotifications(nullEvent);

      // then
      verify(subscriptionRepository, never()).findAllByInterestIdInWithUserAndInterest(anyList());
      verify(notificationRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("구독자가 한 명도 없으면 조기 종료된다.")
    void fail_create_notification_no_subscribers() {
      // given
      UUID interestId = UUID.randomUUID();
      BulkArticleRegisteredEvent event = new BulkArticleRegisteredEvent(
          List.of(new BulkArticleRegisteredEvent.InterestArticleCount(interestId, "테스트 관심사", 5))
      );

      given(subscriptionRepository.findAllByInterestIdInWithUserAndInterest(anyList()))
          .willReturn(Collections.emptyList());

      // when
      notificationService.createInterestNotifications(event);

      // then
      verify(notificationRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("구독자가 존재하면 알림을 대량 생성하여 저장한다.")
    void success_create_article_notifications() {
      // given
      UUID interestId1 = UUID.randomUUID();
      UUID interestId2 = UUID.randomUUID(); // 구독자가 없는 관심사

      BulkArticleRegisteredEvent event = new BulkArticleRegisteredEvent(List.of(
          new BulkArticleRegisteredEvent.InterestArticleCount(interestId1, "테스트 관심사1", 5),
          new BulkArticleRegisteredEvent.InterestArticleCount(interestId2, "테스트 관심사2", 2)
      ));

      Interest mockInterest = mock(Interest.class);
      given(mockInterest.getId()).willReturn(interestId1);

      User mockUser = mock(User.class);
      Subscription mockSubscription = mock(Subscription.class);
      given(mockSubscription.getInterest()).willReturn(mockInterest);
      given(mockSubscription.getUser()).willReturn(mockUser);

      // 테스트 관심사1에 대해서만 1명의 구독자가 있다고 가정
      given(subscriptionRepository.findAllByInterestIdInWithUserAndInterest(anyList()))
          .willReturn(List.of(mockSubscription));

      // when
      notificationService.createInterestNotifications(event);

      // then
      verify(notificationRepository, times(1)).saveAll(interestNotificationListCaptor.capture());

      List<InterestNotification> savedNotifications = interestNotificationListCaptor.getValue();
      assertThat(savedNotifications).hasSize(1);
      assertThat(savedNotifications.get(0).getContent()).isEqualTo(
          "[테스트 관심사1]와 관련된 기사가 5건 등록되었습니다.");
    }
  }

  @Nested
  @DisplayName("댓글 좋아요 알림 테스트")
  class CreateCommentLikeNotificationTest {

    @Test
    @DisplayName("유저를 찾을 수 없으면 UserNotFoundException이 발생한다.")
    void fail_create_notification_UserNotFound() {
      // given
      UUID commentId = UUID.randomUUID();
      UUID readerId = UUID.randomUUID();
      given(userRepository.findByIdAndDeletedAtIsNull(readerId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(
          () -> notificationService.createCommentLikeNotification(commentId, readerId, "테스트 닉네임"))
          .isInstanceOf(UserNotFoundException.class);
      verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("댓글을 찾을 수 없으면 CommentNotFoundException이 발생한다.")
    void fail_create_notification_CommentNotFound() {
      // given
      UUID commentId = UUID.randomUUID();
      UUID readerId = UUID.randomUUID();
      User mockUser = mock(User.class);

      given(userRepository.findByIdAndDeletedAtIsNull(readerId)).willReturn(Optional.of(mockUser));
      given(commentRepository.findById(commentId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(
          () -> notificationService.createCommentLikeNotification(commentId, readerId, "테스트 닉네임"))
          .isInstanceOf(CommentNotFoundException.class);
      verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("댓글 작성자와 알림 수신자가 일치하지 않으면 NotificationReceiverMismatchException 발생한다.")
    void fail_create_notification_ReceiverMismatch() {
      // given
      UUID commentId = UUID.randomUUID();
      UUID readerId = UUID.randomUUID();
      UUID realOwnerId = UUID.randomUUID(); // 실제 작성자의 다른 ID

      User mockReader = mock(User.class);
      given(mockReader.getId()).willReturn(readerId); // 수신자 ID

      User mockOwner = mock(User.class);
      given(mockOwner.getId()).willReturn(realOwnerId); // 실제 작성자 ID

      Comment mockComment = mock(Comment.class);
      given(mockComment.getUser()).willReturn(mockOwner);

      given(userRepository.findByIdAndDeletedAtIsNull(readerId)).willReturn(
          Optional.of(mockReader));
      given(commentRepository.findById(commentId)).willReturn(Optional.of(mockComment));

      // when & then
      assertThatThrownBy(
          () -> notificationService.createCommentLikeNotification(commentId, readerId, "테스트 닉네임"))
          .isInstanceOf(NotificationReceiverMismatchException.class)
          .hasMessageContaining("댓글 작성자와 알림 수신자가 일치하지 않습니다.");

      verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("정상적으로 댓글 좋아요 알림을 생성하고 저장한다.")
    void success_create_comment_notifications() {
      // given
      UUID commentId = UUID.randomUUID();
      UUID readerId = UUID.randomUUID();
      String likerNickname = "테스트 닉네임";

      User mockUser = mock(User.class);
      given(mockUser.getId()).willReturn(readerId);

      Comment mockComment = mock(Comment.class);
      given(mockComment.getUser()).willReturn(mockUser);

      given(userRepository.findByIdAndDeletedAtIsNull(readerId)).willReturn(Optional.of(mockUser));
      given(commentRepository.findById(commentId)).willReturn(Optional.of(mockComment));

      // when
      notificationService.createCommentLikeNotification(commentId, readerId, likerNickname);

      // then
      verify(notificationRepository, times(1)).save(commentNotificationCaptor.capture());

      CommentNotification savedNotification = commentNotificationCaptor.getValue();
      assertThat(savedNotification.getContent()).isEqualTo("[테스트 닉네임]님이 나의 댓글을 좋아합니다.");
      assertThat(savedNotification.getUser()).isEqualTo(mockUser);
      assertThat(savedNotification.getComment()).isEqualTo(mockComment);
    }
  }

  @Nested
  @DisplayName("알림 단건 확인 테스트")
  class ConfirmNotificationTest {

    @Test
    @DisplayName("알림을 찾을 수 없으면 NotificationNotFoundException이 발생한다.")
    void fail_confirm_NotificationNotFound() {
      // given
      UUID notificationId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      given(notificationRepository.findById(notificationId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> notificationService.confirmNotification(notificationId, userId))
          .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    @DisplayName("알림의 소유자가 아니면 NotificationAccessDeniedException이 발생한다.")
    void fail_confirm_AccessDenied() {
      // given
      UUID notificationId = UUID.randomUUID();
      UUID requesterId = UUID.randomUUID();
      UUID ownerId = UUID.randomUUID(); // 요청자와 다른 소유자 ID

      User mockOwner = mock(User.class);
      given(mockOwner.getId()).willReturn(ownerId);

      Notification mockNotification = mock(Notification.class);
      given(mockNotification.getUser()).willReturn(mockOwner);

      given(notificationRepository.findById(notificationId)).willReturn(Optional.of(mockNotification));

      // when & then
      assertThatThrownBy(() -> notificationService.confirmNotification(notificationId, requesterId))
          .isInstanceOf(NotificationAccessDeniedException.class);
      verify(mockNotification, never()).confirm();
    }

    @Test
    @DisplayName("정상적으로 알림을 읽음 처리한다.")
    void success_confirm_notification() {
      // given
      UUID notificationId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();

      User mockUser = mock(User.class);
      given(mockUser.getId()).willReturn(userId);

      Notification mockNotification = mock(Notification.class);
      given(mockNotification.getUser()).willReturn(mockUser);

      given(notificationRepository.findById(notificationId)).willReturn(Optional.of(mockNotification));

      // when
      notificationService.confirmNotification(notificationId, userId);

      // then
      verify(mockNotification, times(1)).confirm();
    }
  }

  @Nested
  @DisplayName("알림 전체 확인 테스트")
  class ConfirmAllNotificationsTest {

    @Test
    @DisplayName("유저의 모든 안 읽은 알림을 읽음 처리한다.")
    void success_confirm_all_notifications() {
      // given
      UUID userId = UUID.randomUUID();
      given(notificationRepository.confirmAllByUserId(any(UUID.class), any(java.time.Instant.class)))
          .willReturn(5);

      // when
      notificationService.confirmAllNotifications(userId);

      // then
      verify(notificationRepository, times(1)).confirmAllByUserId(any(UUID.class), any(java.time.Instant.class));
    }
  }

  @Nested
  @DisplayName("안 읽은 알림 목록 조회 테스트")
  class GetUnconfirmedNotificationsTest {

    @Test
    @DisplayName("다음 페이지가 없는 경우(hasNext=false)를 정상적으로 반환한다.")
    void success_get_notifications_has_no_next() {
      // given
      UUID userId = UUID.randomUUID();
      int limit = 10;

      // limit보다 적은 2개의 알림이 조회되었다고 가정
      Notification mockNoti1 = mock(Notification.class);
      Notification mockNoti2 = mock(Notification.class);
      List<Notification> mockNotifications = new ArrayList<>(List.of(mockNoti1, mockNoti2));

      given(notificationQueryRepository.findUnconfirmedByCursor(userId, null, null, limit))
          .willReturn(mockNotifications);
      given(notificationQueryRepository.countUnconfirmedByUserId(userId)).willReturn(2L);

      com.codeit.monew.domain.notification.dto.NotificationDto mockDto1 = mock(com.codeit.monew.domain.notification.dto.NotificationDto.class);
      com.codeit.monew.domain.notification.dto.NotificationDto mockDto2 = mock(com.codeit.monew.domain.notification.dto.NotificationDto.class);
      given(mockDto2.id()).willReturn(UUID.randomUUID());
      given(mockDto2.createdAt()).willReturn(java.time.Instant.now());

      given(notificationMapper.toDto(mockNoti1)).willReturn(mockDto1);
      given(notificationMapper.toDto(mockNoti2)).willReturn(mockDto2);

      // when
      com.codeit.monew.domain.notification.dto.NotificationListDto result =
          notificationService.getUnconfirmedNotifications(userId, null, null, limit);

      // then
      assertThat(result.content()).hasSize(2);
      assertThat(result.hasNext()).isFalse();
      assertThat(result.totalElements()).isEqualTo(2L);
      assertThat(result.nextCursor()).isNotNull();
      assertThat(result.nextAfter()).isNotNull();
    }

    @Test
    @DisplayName("다음 페이지가 있는 경우(hasNext=true) 초과 데이터를 자르고 반환한다.")
    void success_get_notifications_has_next() {
      // given
      UUID userId = UUID.randomUUID();
      int limit = 2; // limit을 2로 설정

      // 데이터베이스에서는 limit + 1 개인 3개를 가져왔다고 가정
      Notification mockNoti1 = mock(Notification.class);
      Notification mockNoti2 = mock(Notification.class);
      Notification mockNoti3 = mock(Notification.class); // 잘려나갈 데이터
      List<Notification> mockNotifications = new ArrayList<>(List.of(mockNoti1, mockNoti2, mockNoti3));

      given(notificationQueryRepository.findUnconfirmedByCursor(userId, null, null, limit))
          .willReturn(mockNotifications);
      given(notificationQueryRepository.countUnconfirmedByUserId(userId)).willReturn(5L);

      com.codeit.monew.domain.notification.dto.NotificationDto mockDto1 = mock(com.codeit.monew.domain.notification.dto.NotificationDto.class);
      com.codeit.monew.domain.notification.dto.NotificationDto mockDto2 = mock(com.codeit.monew.domain.notification.dto.NotificationDto.class);
      given(mockDto2.id()).willReturn(UUID.randomUUID());
      given(mockDto2.createdAt()).willReturn(java.time.Instant.now());

      given(notificationMapper.toDto(mockNoti1)).willReturn(mockDto1);
      given(notificationMapper.toDto(mockNoti2)).willReturn(mockDto2);

      // when
      com.codeit.monew.domain.notification.dto.NotificationListDto result =
          notificationService.getUnconfirmedNotifications(userId, null, null, limit);

      // then
      assertThat(result.content()).hasSize(2); // 3개 중 1개가 잘려서 2개 반환
      assertThat(result.hasNext()).isTrue();
      assertThat(result.totalElements()).isEqualTo(5L);
    }
  }

  @Nested
  @DisplayName("오래된 알림 삭제 배치 테스트")
  class CleanUpOldNotificationsTest {

    @Test
    @DisplayName("7일이 지난 읽은 알림을 정상적으로 삭제한다.")
    void success_cleanup_old_notifications() {
      // given
      given(notificationRepository.deleteOldConfirmedNotifications(any(java.time.Instant.class)))
          .willReturn(10); // 10건 삭제되었다고 가정

      // when
      notificationService.cleanUpOldNotifications();

      // then
      verify(notificationRepository, times(1)).deleteOldConfirmedNotifications(any(java.time.Instant.class));
    }
  }
}