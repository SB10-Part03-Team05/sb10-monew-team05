package com.codeit.monew.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.monew.domain.user.dto.UserDto;
import com.codeit.monew.domain.user.dto.UserRegisterRequest;
import com.codeit.monew.domain.user.dto.UserUpdateRequest;
import com.codeit.monew.domain.user.service.UserService;
import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.GlobalExceptionHandler;
import com.codeit.monew.global.exception.user.DuplicateEmailException;
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
    @DisplayName("유효한 요청 시 200 상태코드와 수정된 사용자 정보가 반환된다.")
    void should_update_user_success_and_return_200() throws Exception {
      // given
      UserUpdateRequest request = new UserUpdateRequest("newNickName");
      UUID userId = UUID.randomUUID();
      Instant createdAt = Instant.now();
      UserDto responseDto = new UserDto(userId, "test@email.com", "newNickName", createdAt);

      given(userService.update(any(UUID.class), any(UUID.class), any(UserUpdateRequest.class))).willReturn(responseDto);
      // when, then
      mockMvc.perform(patch("/api/users/" + userId)
              .header("Monew-Request-User-ID", userId)
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
              .header("Monew-Request-User-ID", userId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("요청자 ID와 수정할 사용자의 ID가 다르면 403 상태 코드가 반환된다.")
    void should_fail_update_user_when_id_mismatch() throws Exception {
      // given
      UserUpdateRequest request = new UserUpdateRequest("newNickName");
      UUID userId = UUID.randomUUID();
      UUID requestUserId = UUID.randomUUID();

      given(userService.update(any(UUID.class), any(UUID.class), any(UserUpdateRequest.class))).willThrow(
          new UserAccessDeniedException(requestUserId)
      );
      // when, then
      mockMvc.perform(patch("/api/users/" + userId)
              .header("Monew-Request-User-ID", requestUserId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("수정할 사용자가 존재하지 않으면 404 상태 코드가 반환된다.")
    void should_fail_update_user_fail_when_user_not_found() throws Exception {
      // given
      UserUpdateRequest request = new UserUpdateRequest("newNickName");
      UUID userId = UUID.randomUUID();

      given(userService.update(any(UUID.class), any(UUID.class), any(UserUpdateRequest.class))).willThrow(
          new UserNotFoundException(userId)
      );
      // when, then
      mockMvc.perform(patch("/api/users/" + userId)
              .header("Monew-Request-User-ID", userId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404));
    }
  }
}