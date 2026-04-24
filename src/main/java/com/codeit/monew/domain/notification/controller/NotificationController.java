package com.codeit.monew.domain.notification.controller;

import com.codeit.monew.domain.notification.dto.NotificationListDto;
import com.codeit.monew.domain.notification.service.NotificationService;
import com.codeit.monew.global.exception.common.InvalidParameterException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)"),
      @ApiResponse(responseCode = "403", description = "해당 알림에 접근할 권한이 없음"),
      @ApiResponse(responseCode = "404", description = "알림 정보 없음"),
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

  // 전체 알림 확인
  @Operation(summary = "전체 알림 확인", description = "전체 알림을 한번에 확인합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "전체 알림 확인 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)"),
      @ApiResponse(responseCode = "404", description = "사용자 정보 없음"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @PatchMapping
  public ResponseEntity<Void> confirmAll(
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    notificationService.confirmAllNotifications(userId);
    return ResponseEntity.ok().build();
  }

  // 미확인 알림 목록 조회
  @GetMapping
  @Operation(summary = "알림 목록 조회", description = "알림 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (정렬 기준 오류, 페이지네이션 파라미터 오류 등)"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  public ResponseEntity<NotificationListDto> getUnconfirmedNotifications(
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") UUID userId,
      @Parameter(description = "커서 값") @RequestParam(required = false) String cursor,
      @Parameter(description = "보조 커서(createdAt) 값") @RequestParam(required = false) Instant after,
      @Parameter(description = "커서 페이지 크기", example = "50") @RequestParam(defaultValue = "50") int limit
  ) {
    UUID cursorId = null;
    if (cursor != null) {
      try {
        cursorId = UUID.fromString(cursor);
      } catch (IllegalArgumentException e) {
        throw new InvalidParameterException("cursor", cursor);
      }
    }
    NotificationListDto response = notificationService.getUnconfirmedNotifications(userId, after, cursorId, limit);
    return ResponseEntity.ok(response);
  }
}
