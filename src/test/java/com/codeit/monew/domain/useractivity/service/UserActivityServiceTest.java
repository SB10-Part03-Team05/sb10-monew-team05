package com.codeit.monew.domain.useractivity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.useractivity.dto.UserActivityDto;
import com.codeit.monew.domain.useractivity.entity.UserActivity;
import com.codeit.monew.domain.useractivity.entity.UserActivity.ArticleViewInfo;
import com.codeit.monew.domain.useractivity.entity.UserActivity.CommentInfo;
import com.codeit.monew.domain.useractivity.entity.UserActivity.CommentLikeInfo;
import com.codeit.monew.domain.useractivity.entity.UserActivity.SubscriptionInfo;
import com.codeit.monew.domain.useractivity.mapper.UserActivityMapper;
import com.codeit.monew.domain.useractivity.repository.UserActivityRepository;
import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

  @Mock
  private UserActivityRepository userActivityRepository;

  @Mock
  private UserActivityMapper userActivityMapper;

  @InjectMocks
  private UserActivityService userActivityService;

  @Nested
  @DisplayName("사용자 활동 내역 조회 테스트")
  class getUserActivity {

    @Test
    @DisplayName("사용자 활동 내역 조회에 성공해야 한다.")
    void should_get_user_activity_success() {
      // given
      UUID userId = UUID.randomUUID();
      String email = "test@email.com";
      String nickname = "testNickname";
      Instant createdAt = Instant.now();

      UserActivity userActivity = new UserActivity();
      userActivity.setId(userId.toString());
      userActivity.setEmail(email);
      userActivity.setNickname(nickname);
      userActivity.setCreatedAt(createdAt);

      UserActivityDto userActivityDto = new UserActivityDto(
          userId.toString(),
          email,
          nickname,
          createdAt,
          List.of(),
          List.of(),
          List.of(),
          List.of()
      );

      given(userActivityRepository.findById(any(String.class))).willReturn(Optional.of(userActivity));
      given(userActivityMapper.toDto(any(UserActivity.class))).willReturn(userActivityDto);

      // when
      UserActivityDto resultDto = userActivityService.getUserActivity(userId);

      // then
      assertThat(resultDto.id()).isEqualTo(userId.toString());
      assertThat(resultDto.email()).isEqualTo(email);
      assertThat(resultDto.nickname()).isEqualTo(nickname);
      assertThat(resultDto.subscriptions()).hasSize(0);
      assertThat(resultDto.comments()).hasSize(0);
      assertThat(resultDto.commentLikes()).hasSize(0);
      assertThat(resultDto.articleViews()).hasSize(0);
      verify(userActivityRepository).findById(userId.toString());
      verify(userActivityMapper).toDto(any(UserActivity.class));

    }

    @Test
    @DisplayName("사용자가 존재하지 않으면 UserNotFound 예외가 발생한다.")
    void should_fail_get_user_activity_success() {
      // given
      UUID userId = UUID.randomUUID();

      given(userActivityRepository.findById(any(String.class))).willReturn(Optional.empty());

      // when, then
      UserNotFoundException exception = assertThrows(UserNotFoundException.class,
          () -> userActivityService.getUserActivity(userId));
      assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
      verify(userActivityMapper, never()).toDto(any(UserActivity.class));

    }
  }
}