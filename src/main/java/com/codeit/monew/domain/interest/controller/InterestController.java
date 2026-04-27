package com.codeit.monew.domain.interest.controller;

import com.codeit.monew.domain.interest.dto.request.InterestRegisterRequest;
import com.codeit.monew.domain.interest.dto.request.InterestUpdateRequest;
import com.codeit.monew.domain.interest.dto.response.CursorPageResponseInterestDto;
import com.codeit.monew.domain.interest.dto.response.InterestDto;
import com.codeit.monew.domain.interest.dto.response.SubscriptionDto;
import com.codeit.monew.domain.interest.service.InterestService;
import com.codeit.monew.domain.user.dto.UserDto;
import com.codeit.monew.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
@Validated
@Tag(name = "관심사 관리", description = "관심사 관련 API")
public class InterestController {

  private final InterestService interestService;

  // 1. 관심사 등록
  @PostMapping
  @Operation(summary = "관심사 등록", description = "새로운 관심사를 등록합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "등록 성공", content = @Content(schema = @Schema(implementation = InterestDto.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "409", description = "유사 관심사 중복", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<InterestDto> register(
      @RequestBody @Valid InterestRegisterRequest request
  ) {
    InterestDto response = interestService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  // 2. 관심사 수정
  @PatchMapping("/{interestId}")
  @Operation(summary = "관심사 정보 수정", description = "관심사의 키워드를 수정합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "수정 성공", content = @Content(schema = @Schema(implementation = InterestDto.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "관심사 정보 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<InterestDto> update(
      @PathVariable UUID interestId,
      @RequestBody @Valid InterestUpdateRequest request
  ) {
    InterestDto response = interestService.update(interestId, request);
    return ResponseEntity.ok(response);
  }

  // 3. 관심사 삭제
  @DeleteMapping("/{interestId}")
  @Operation(summary = "관심사 물리 삭제", description = "관심사를 물리적으로 삭제합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "삭제 성공", content = @Content()),
      @ApiResponse(responseCode = "404", description = "관심사 정보 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<Void> delete(
      @PathVariable UUID interestId
  ) {
    interestService.delete(interestId);
    return ResponseEntity.noContent().build();
  }

  // 4. 관심사 목록 조회
  @GetMapping
  @Operation(summary = "관심사 목록 조회", description = "조건에 맞는 관심사 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = CursorPageResponseInterestDto.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (정렬 기준 오류, 페이지네이션 파라미터 오류 등)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<CursorPageResponseInterestDto> getList(
      @RequestParam(required = false) String keyword,
      @RequestParam String orderBy,
      @RequestParam String direction,
      @RequestParam(required = false) String cursor,
      @RequestParam @Min(1) @Max(100) int limit, // 과도한 조회/예외 가능성 방지를 위한 가드 추가
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    CursorPageResponseInterestDto response = interestService.getList(
        keyword, orderBy, direction, cursor, limit, userId
    );
    return ResponseEntity.ok(response);
  }

  // 5. 관심사 구독
  @PostMapping("/{interestId}/subscriptions")
  @Operation(summary = "관심사 구독", description = "관심사를 구독합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "구독 성공", content = @Content(schema = @Schema(implementation = SubscriptionDto.class))),
      @ApiResponse(responseCode = "404", description = "관심사 정보 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<SubscriptionDto> subscribe(
      @PathVariable UUID interestId,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    SubscriptionDto response = interestService.subscribe(interestId, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  // 6. 관심사 구독 취소
  @DeleteMapping("/{interestId}/subscriptions")
  @Operation(summary = "관심사 구독 취소", description = "관심사를 구독을 취소합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "구독 취소 성공", content = @Content()),
      @ApiResponse(responseCode = "404", description = "관심사 정보 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<Void> unsubscribe(
      @PathVariable UUID interestId,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    interestService.unsubscribe(interestId, userId);
    return ResponseEntity.noContent().build();
  }

}
