package com.codeit.monew.domain.useractivity.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.monew.domain.useractivity.dto.UserActivityDto;
import com.codeit.monew.domain.useractivity.service.UserActivityService;
import com.codeit.monew.global.exception.GlobalExceptionHandler;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserActivityController.class)
@Import(GlobalExceptionHandler.class)
class UserActivityControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserActivityService userActivityService;

  @Nested
  @DisplayName("사용자 활동 내역 조회 API 테스트")
  class getUserActivity {
    @Test
    @DisplayName("유효한 활동 내역 조회 요청 시 200 상태코드와 활동 내역 정보가 반환된다.")
    void should_get_user_activity_and_return_200() throws Exception {
      // given
      UUID userId = UUID.randomUUID();
      Instant createdAt = Instant.now();
      UserActivityDto responseDto = new UserActivityDto(
          userId.toString(),
          "test@email.com",
          "testNickname",
          createdAt,
          List.of(),
          List.of(),
          List.of(),
          List.of()
      );

      given(userActivityService.getUserActivity(any(UUID.class))).willReturn(responseDto);
      // when, them
      mockMvc.perform(get("/api/user-activities/" + userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(userId.toString()))
          .andExpect(jsonPath("$.email").value("test@email.com"))
          .andExpect(jsonPath("$.nickname").value("testNickname"))
          .andExpect(jsonPath("$.createdAt").value(createdAt.toString()));
    }

    @Test
    @DisplayName("조회할 사용자가 존재하지 않으면 404 상태 코드가 반환된다.")
    void should_fail_get_user_activity_and_return_404() throws Exception {
      // given
      UUID userId = UUID.randomUUID();

      given(userActivityService.getUserActivity(any(UUID.class)))
          .willThrow(new UserNotFoundException(userId));
      // when, them
      mockMvc.perform(get("/api/user-activities/" + userId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404));
    }

  }

}