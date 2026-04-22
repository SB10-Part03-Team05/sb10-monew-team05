package com.codeit.monew.domain.useractivity.controller;

import com.codeit.monew.domain.user.dto.UserDto;
import com.codeit.monew.domain.user.service.UserService;
import com.codeit.monew.domain.useractivity.dto.UserActivityDto;
import com.codeit.monew.domain.useractivity.service.UserActivityService;
import com.codeit.monew.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-activities")
@RequiredArgsConstructor
@Tag(name = "사용자 활동 내역 관리", description = "사용자 활동 내역 관련 API")
public class UserActivityController {

  private final UserActivityService userActivityService;

  @GetMapping("/{userId}")
  @Operation(summary = "사용자 활동 내역 조회", description = "사용자 ID로 활동 내역을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "사용자 활동 내역 조회 성공", content = @Content(schema = @Schema(implementation = UserDto.class))),
      @ApiResponse(responseCode = "404", description = "사용자 정보 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<UserActivityDto> getUserActivity(
      @PathVariable UUID userId
  ) {
    UserActivityDto dto = userActivityService.getUserActivity(userId);
    return ResponseEntity.status(HttpStatus.OK).body(dto);
  }
}
