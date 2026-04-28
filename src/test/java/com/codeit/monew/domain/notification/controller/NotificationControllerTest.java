package com.codeit.monew.domain.notification.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.monew.domain.notification.dto.NotificationListDto;
import com.codeit.monew.domain.notification.service.NotificationService;
import com.codeit.monew.global.exception.common.InvalidParameterException;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private NotificationService notificationService;

  private final String HEADER_USER_ID = "Monew-Request-User-ID";
  private final UUID testUserId = UUID.randomUUID();

  @Nested
  @DisplayName("단건 알림 확인 API 테스트")
  class ConfirmTest {

    @Test
    @DisplayName("정상적인 요청 시 200 OK를 반환한다.")
    void success_confirm_notification() throws Exception {
      // given
      UUID notificationId = UUID.randomUUID();
      doNothing().when(notificationService).confirmNotification(notificationId, testUserId);

      // when & then
      mockMvc.perform(patch("/api/notifications/{notificationId}", notificationId)
              .header(HEADER_USER_ID, testUserId.toString()))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("헤더에 유저 ID가 없으면 400 Bad Request를 반환한다.")
    void fail_confirm_notification_missing_header() throws Exception {
      // given
      UUID notificationId = UUID.randomUUID();

      // when & then
      mockMvc.perform(patch("/api/notifications/{notificationId}", notificationId))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("전체 알림 확인 API 테스트")
  class ConfirmAllTest {

    @Test
    @DisplayName("정상적인 요청 시 200 OK를 반환한다.")
    void success_confirmAll_notification() throws Exception {
      // given
      doNothing().when(notificationService).confirmAllNotifications(testUserId);

      // when & then
      mockMvc.perform(patch("/api/notifications")
              .header(HEADER_USER_ID, testUserId.toString()))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("미확인 알림 목록 조회 API 테스트")
  class GetUnconfirmedNotificationsTest {

    @Test
    @DisplayName("정상적인 파라미터로 요청 시 200 OK와 알림 목록을 반환한다.")
    void success_getUnconfirmedNotifications() throws Exception {
      // given
      UUID cursorId = UUID.randomUUID();
      Instant after = Instant.now();
      int limit = 20;

      NotificationListDto mockResponse = new NotificationListDto(
          Collections.emptyList(),
          cursorId.toString(),
          after,
          0,
          0L,
          false
      );

      given(notificationService.getUnconfirmedNotifications(eq(testUserId), any(), any(), eq(limit)))
          .willReturn(mockResponse);

      // when & then
      mockMvc.perform(get("/api/notifications")
              .header(HEADER_USER_ID, testUserId.toString())
              .param("cursor", cursorId.toString())
              .param("after", after.toString())
              .param("limit", String.valueOf(limit)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.nextCursor").value(cursorId.toString()))
          .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("잘못된 형식의 UUID 커서 값을 전달하면 InvalidParameterException이 발생한다.")
    void fail_getUnconfirmedNotifications_invalid_cursor() throws Exception {
      // given
      String invalidCursor = "invalid-uuid-format";

      // when & then
      mockMvc.perform(get("/api/notifications")
              .header(HEADER_USER_ID, testUserId.toString())
              .param("cursor", invalidCursor))
          .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidParameterException));
    }
  }
}