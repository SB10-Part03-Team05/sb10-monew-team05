package com.codeit.monew.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.monew.domain.user.dto.UserDto;
import com.codeit.monew.domain.user.dto.UserLoginRequest;
import com.codeit.monew.domain.user.dto.UserRegisterRequest;
import com.codeit.monew.domain.user.dto.UserUpdateRequest;
import com.codeit.monew.domain.user.service.UserService;
import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.GlobalExceptionHandler;
import com.codeit.monew.global.exception.user.DuplicateEmailException;
import com.codeit.monew.global.exception.user.PasswordMismatchException;
import com.codeit.monew.global.exception.user.UserAccessDeniedException;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

  @Nested
  @DisplayName("사용자 회원가입 API 테스트")
  class createUser {

    @Test
    @DisplayName("유효한 회원가입 요청 시 201 상태코드와 등록된 사용자 정보가 반환된다.")
    void should_register_user_success_and_return_201() throws Exception {
      // given
      UserRegisterRequest request = new UserRegisterRequest("test@email.com", "testNickname",
          "testPassword1!");
      UUID userId = UUID.randomUUID();
      Instant createdAt = Instant.now();
      UserDto responseDto = new UserDto(userId, "test@email.com", "testNickname", createdAt);

      given(userService.register(any(UserRegisterRequest.class))).willReturn(responseDto);
      // when, then
      mockMvc.perform(post("/api/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(userId.toString()))
          .andExpect(jsonPath("$.email").value(request.email()))
          .andExpect(jsonPath("$.nickname").value(request.nickname()));
    }

    @Test
    @DisplayName("중복된 이메일로 회원가입 요청 시 409 상태 코드와 DuplicateEmail 예외가 발생한다.")
    void should_fail_register_when_email_duplicated() throws Exception {
      // given
      UserRegisterRequest request = new UserRegisterRequest("test@email.com", "testNickname",
          "testPassword1!");

      given(userService.register(any(UserRegisterRequest.class))).willThrow(
          new DuplicateEmailException(request.email()));

      // when, then
      mockMvc.perform(post("/api/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_EMAIL.toString()))
          .andExpect(jsonPath("$.status").value(409))
          .andExpect(
              jsonPath("$.exceptionType").value(DuplicateEmailException.class.getSimpleName()));
    }

    @Test
    @DisplayName("잘못된 이메일 형식으로 회원가입 요청 시 400 상태 코드가 반환된다.")
    void should_fail_register_when_email_invalid() throws Exception {
      // given
      UserRegisterRequest request = new UserRegisterRequest("invalid-email", "testNickname",
          "testPassword1!");
      // when, then
      mockMvc.perform(post("/api/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400));
    }
  }

  @Nested
  @DisplayName("사용자 수정 API 테스트")
  class updateUser {

    @Test
    @DisplayName("유효한 수정 요청 시 200 상태코드와 수정된 사용자 정보가 반환된다.")
    void should_update_user_success_and_return_200() throws Exception {
      // given
      UserUpdateRequest request = new UserUpdateRequest("newNickName");
      UUID userId = UUID.randomUUID();
      Instant createdAt = Instant.now();
      UserDto responseDto = new UserDto(userId, "test@email.com", "newNickName", createdAt);

      given(userService.update(any(UUID.class), any(UserUpdateRequest.class))).willReturn(
          responseDto);
      // when, then
      mockMvc.perform(patch("/api/users/" + userId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(userId.toString()))
          .andExpect(jsonPath("$.nickname").value(request.nickname()));
    }

    @Test
    @DisplayName("잘못된 형식으로 수정 요청 시 400 상태 코드가 반환된다.")
    void should_fail_update_user_when_nickname_invalid() throws Exception {
      // given
      UserUpdateRequest request = new UserUpdateRequest("");
      UUID userId = UUID.randomUUID();

      // when, then
      mockMvc.perform(patch("/api/users/" + userId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("수정할 사용자가 존재하지 않으면 404 상태 코드가 반환된다.")
    void should_fail_update_user_when_user_not_found() throws Exception {
      // given
      UserUpdateRequest request = new UserUpdateRequest("newNickName");
      UUID userId = UUID.randomUUID();

      given(userService.update(any(UUID.class), any(UserUpdateRequest.class))).willThrow(
          new UserNotFoundException(userId)
      );
      // when, then
      mockMvc.perform(patch("/api/users/" + userId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404));
    }
  }

  @Nested
  @DisplayName("사용자 논리 삭제 API 테스트")
  class softDeleteUser {

    @Test
    @DisplayName("유효한 삭제 요청 시 204 상태코드가 반환된다.")
    void should_delete_user_and_return_204() throws Exception {
      // given
      UUID userId = UUID.randomUUID();

      willDoNothing().given(userService).softDelete(userId);

      // when, then
      mockMvc.perform(delete("/api/users/" + userId))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("논리 삭제할 사용자가 존재하지 않으면 404 상태 코드가 반환된다.")
    void should_fail_delete_user_when_user_not_found() throws Exception {
      // given
      UUID userId = UUID.randomUUID();

      willThrow(new UserNotFoundException(userId)).given(userService).softDelete(any(UUID.class));

      // when, then
      mockMvc.perform(delete("/api/users/" + userId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404));
    }
  }

  @Nested
  @DisplayName("사용자 물리 삭제 API 테스트")
  class hardDeleteUser {

    @Test
    @DisplayName("유효한 삭제 요청 시 204 상태코드가 반환된다.")
    void should_delete_user_and_return_204() throws Exception {
      // given
      UUID userId = UUID.randomUUID();

      willDoNothing().given(userService).hardDelete(userId);

      // when, then
      mockMvc.perform(delete("/api/users/" + userId + "/hard"))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("물리 삭제할 사용자가 존재하지 않으면 404 상태 코드가 반환된다.")
    void should_fail_delete_user_when_user_not_found() throws Exception {
      // given
      UUID userId = UUID.randomUUID();

      willThrow(new UserNotFoundException(userId)).given(userService).hardDelete(any(UUID.class));

      // when, then
      mockMvc.perform(delete("/api/users/" + userId + "/hard"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404));
    }
  }

  @Nested
  @DisplayName("사용자 로그인 API 테스트")
  class loginUser {

    @Test
    @DisplayName("유효한 로그인 요청 시 200 상태코드가 반환된다.")
    void should_login_user_and_return_200() throws Exception {
      // given
      UserLoginRequest request = new UserLoginRequest("test@email.com", "testPassword1!");
      UUID userId = UUID.randomUUID();
      Instant createdAt = Instant.now();
      UserDto responseDto = new UserDto(userId, "test@email.com", "testNickname", createdAt);

      given(userService.login(request)).willReturn(responseDto);
      // when, then
      mockMvc.perform(post("/api/users/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(userId.toString()))
          .andExpect(jsonPath("$.email").value(request.email()));
    }

    @Test
    @DisplayName("로그인할 사용자가 존재하지 않으면 404 상태 코드가 반환된다.")
    void should_fail_login_user_when_user_not_found() throws Exception {
      // given
      UserLoginRequest request = new UserLoginRequest("test@email.com", "testPassword1!");

      given(userService.login(any(UserLoginRequest.class))).willThrow(
          new UserNotFoundException(request.email()));
      // when, then
      mockMvc.perform(post("/api/users/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("잘못된 이메일 형식으로 로그인 요청 시 400 상태 코드가 반환된다.")
    void should_fail_login_user_when_email_invalid() throws Exception {
      // given
      UserLoginRequest request = new UserLoginRequest("invalid-email", "testPassword1!");

      // when, then
      mockMvc.perform(post("/api/users/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("비밀번호가 맞지 않을 시 401 상태 코드가 반환된다.")
    void should_fail_login_user_when_password_mismatch() throws Exception {
      // given
      UserLoginRequest request = new UserLoginRequest("test@email.com", "testPassword1!");

      given(userService.login(any(UserLoginRequest.class))).willThrow(
          new PasswordMismatchException(request.email()));
      // when, then
      mockMvc.perform(post("/api/users/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.status").value(401));
    }
  }
}