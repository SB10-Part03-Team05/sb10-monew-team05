package com.codeit.monew.domain.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.user.dto.UserDto;
import com.codeit.monew.domain.user.dto.UserRegisterRequest;
import com.codeit.monew.domain.user.dto.UserUpdateRequest;
import com.codeit.monew.domain.user.entity.User;
import com.codeit.monew.domain.user.mapper.UserMapper;
import com.codeit.monew.domain.user.repository.UserRepository;
import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.user.DuplicateEmailException;
import com.codeit.monew.global.exception.user.UserAccessDeniedException;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @InjectMocks
  private UserService userService;

  private User createUser(UUID userId, String email, String nickname, String password) {
    User user = new User(email, nickname, password);

    if (userId == null) {
      ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    } else {
      ReflectionTestUtils.setField(user, "id", userId);
    }

    return user;
  }

  @Nested
  @DisplayName("사용자 회원가입 테스트")
  class register {

    @Test
    @DisplayName("사용자 회원가입에 성공해야 한다.")
    void should_register_user_success() {
      // given
      UserRegisterRequest request = new UserRegisterRequest("test@email.com", "testNickname",
          "testPassword1!");
      UUID userId = UUID.randomUUID();
      Instant createdAt = Instant.now();
      User user = createUser(userId, request.email(), request.nickname(), request.password());
      UserDto expectedUserDto = new UserDto(user.getId(), user.getEmail(), user.getNickname(),
          createdAt);

      given(userRepository.existsByEmail(request.email())).willReturn(false);
      given(userMapper.toDto(any(User.class))).willReturn(expectedUserDto);
      // when
      UserDto result = userService.register(request);

      // then
      assertEquals(expectedUserDto.id(), result.id());
      assertEquals(expectedUserDto.email(), result.email());
      assertEquals(expectedUserDto.nickname(), result.nickname());

      verify(userRepository).existsByEmail(request.email());
      verify(userRepository).save(any(User.class));
      verify(userMapper).toDto(any(User.class));
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 회원가입을 요청하면 DuplicateEmail 예외가 발생한다.")
    void should_fail_register_when_email_duplicated() {
      // given
      UserRegisterRequest request = new UserRegisterRequest("test@email.com", "testNickname",
          "testPassword1!");

      given(userRepository.existsByEmail(request.email())).willReturn(true);

      // when, then
      DuplicateEmailException exception = assertThrows(DuplicateEmailException.class,
          () -> userService.register(request));
      assertEquals(ErrorCode.DUPLICATE_EMAIL, exception.getErrorCode());
      verify(userRepository).existsByEmail(request.email());
      verify(userRepository, never()).save(any(User.class));
      verify(userMapper, never()).toDto(any(User.class));
    }
  }

  @Nested
  @DisplayName("사용자 수정 테스트")
  class updateUser {

    @Test
    @DisplayName("사용자 닉네임 수정에 성공해야 한다.")
    void should_update_user_nickname_success() {
      // given
      UUID userId = UUID.randomUUID();
      Instant createdAt = Instant.now();
      User user = createUser(userId, "test@email.com", "testNickname",
          "testPassword1!");
      UserUpdateRequest request = new UserUpdateRequest("newNickname");
      UserDto expectedUserDto = new UserDto(user.getId(), user.getEmail(), request.nickname(),
          createdAt);

      given(userRepository.findByIdAndDeletedAtIsNull(any(UUID.class))).willReturn(Optional.of(user));
      given(userMapper.toDto(any(User.class))).willReturn(expectedUserDto);

      // when
      UserDto result = userService.update(userId, userId, request);

      // then
      assertEquals(expectedUserDto.nickname(), result.nickname());
      verify(userMapper).toDto(any(User.class));
    }

    @Test
    @DisplayName("사용자가 존재하지 않으면 UserNotFound 예외가 발생한다.")
    void should_fail_update_user_nickname_when_user_not_found() {
      // given
      UUID userId = UUID.randomUUID();
      UserUpdateRequest request = new UserUpdateRequest("newNickname");

      given(userRepository.findByIdAndDeletedAtIsNull(any(UUID.class))).willReturn(Optional.empty());

      // when, then
      UserNotFoundException exception = assertThrows(UserNotFoundException.class,
          () -> userService.update(userId, userId, request));
      assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
      verify(userMapper, never()).toDto(any(User.class));
    }

    @Test
    @DisplayName("요청자 ID와 수정할 사용자의 ID가 다르면 UserAccessDenied 예외가 발생한다.")
    void should_fail_update_user_nickname_when_id_mismatch() {
      // given
      UUID userId = UUID.randomUUID();
      UUID requestUserId = UUID.randomUUID();
      UserUpdateRequest request = new UserUpdateRequest("newNickname");

      // when, then
      UserAccessDeniedException exception = assertThrows(UserAccessDeniedException.class,
          () -> userService.update(userId, requestUserId, request));
      assertEquals(ErrorCode.USER_ACCESS_DENIED, exception.getErrorCode());
      verify(userMapper, never()).toDto(any(User.class));
    }
  }
}