package com.codeit.monew.domain.notification.controller;

import com.codeit.monew.domain.article.dto.response.ArticleDto;
import com.codeit.monew.domain.notification.service.NotificationService;
import com.codeit.monew.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
  private final NotificationService notificationService;

  // 단건 알림 확인
  @Operation(summary = "알림 확인", description = "알림을 확인합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "알림 확인 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검즘 실패)"),
      @ApiResponse(responseCode = "404", description = "사용자 정보 없음"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @PatchMapping("/{notificationId}")
  public ResponseEntity<Void> confirm(
      @Parameter(description = "알림 ID") @PathVariable UUID notificationId,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    notificationService.confirmNotification(notificationId, userId);
    return ResponseEntity.ok().build();
  }
}
