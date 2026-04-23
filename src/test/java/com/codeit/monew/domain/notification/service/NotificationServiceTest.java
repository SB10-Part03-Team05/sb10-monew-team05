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
import com.codeit.monew.domain.notification.event.BulkArticleRegisteredEvent;
import com.codeit.monew.domain.notification.repository.NotificationRepository;
import com.codeit.monew.domain.interest.entity.Subscription;
import com.codeit.monew.domain.interest.repository.SubscriptionRepository;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.exception.comment.CommentNotFoundException;
import com.codeit.monew.global.exception.user.UserNotFoundException;
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

  @Mock private NotificationRepository notificationRepository;
  @Mock private SubscriptionRepository subscriptionRepository;
  @Mock private UserRepository userRepository;
  @Mock private CommentRepository commentRepository;

  @Captor
  private ArgumentCaptor<List<InterestNotification>> interestNotificationListCaptor;
  @Captor
  private ArgumentCaptor<CommentNotification> commentNotificationCaptor;

  @Nested
  @DisplayName("기사 등록 알림 테스트")
  class CreateInterestNotificationsTest {

    @Test
    @DisplayName("이벤트 데이터가 null이거나 비어있으면 조기 종료된다.")
    void returnEarlyWhenEventDataIsNullOrEmpty() {
      // given
      BulkArticleRegisteredEvent emptyEvent = new BulkArticleRegisteredEvent(Collections.emptyList());

      // when
      notificationService.createInterestNotifications(emptyEvent);

      // then
      verify(subscriptionRepository, never()).findAllByInterestIdInWithUserAndInterest(anyList());
      verify(notificationRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("구독자가 한 명도 없으면 조기 종료된다.")
    void returnEarlyWhenNoSubscribersFound() {
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
    void successfullyCreateAndSaveBulkNotifications() {
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
      assertThat(savedNotifications.get(0).getContent()).isEqualTo("[테스트 관심사1]와 관련된 기사가 5건 등록되었습니다.");
    }
  }

  @Nested
  @DisplayName("댓글 좋아요 알림 테스트")
  class CreateCommentLikeNotificationTest {

    @Test
    @DisplayName("유저를 찾을 수 없으면 UserNotFoundException이 발생한다.")
    void throwExceptionWhenUserNotFound() {
      // given
      UUID commentId = UUID.randomUUID();
      UUID readerId = UUID.randomUUID();
      given(userRepository.findByIdAndDeletedAtIsNull(readerId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> notificationService.createCommentLikeNotification(commentId, readerId, "테스트 닉네임"))
          .isInstanceOf(UserNotFoundException.class);
      verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("댓글을 찾을 수 없으면 CommentNotFoundException이 발생한다.")
    void throwExceptionWhenCommentNotFound() {
      // given
      UUID commentId = UUID.randomUUID();
      UUID readerId = UUID.randomUUID();
      User mockUser = mock(User.class);

      given(userRepository.findByIdAndDeletedAtIsNull(readerId)).willReturn(Optional.of(mockUser));
      given(commentRepository.findById(commentId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> notificationService.createCommentLikeNotification(commentId, readerId, "테스트 닉네임"))
          .isInstanceOf(CommentNotFoundException.class);
      verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("정상적으로 댓글 좋아요 알림을 생성하고 저장한다.")
    void successfullyCreateAndSaveCommentNotification() {
      // given
      UUID commentId = UUID.randomUUID();
      UUID readerId = UUID.randomUUID();
      String likerNickname = "테스트 닉네임";

      User mockUser = mock(User.class);
      Comment mockComment = mock(Comment.class);

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
}