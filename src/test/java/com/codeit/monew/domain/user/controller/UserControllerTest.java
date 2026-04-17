package com.codeit.monew.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.monew.domain.user.dto.UserDto;
import com.codeit.monew.domain.user.dto.UserRegisterRequest;
import com.codeit.monew.domain.user.service.UserService;
import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.GlobalExceptionHandler;
import com.codeit.monew.global.exception.user.DuplicateEmailException;
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
    void success_register_user() throws Exception {
      // given

      UserRegisterRequest request = new UserRegisterRequest("test@email.com", "testNickname",
          "Password123!");
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
    void fail_register_when_email_duplicated() throws Exception {
      // given
      UserRegisterRequest request = new UserRegisterRequest("test@email.com", "testNickname",
          "Password123!");

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
    void fail_register_when_email_invalid() throws Exception {
      // given
      UserRegisterRequest request = new UserRegisterRequest("invalid-email", "testNickname", "Password123!");
      // when, then
      mockMvc.perform(post("/api/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400));
    }
  }
}