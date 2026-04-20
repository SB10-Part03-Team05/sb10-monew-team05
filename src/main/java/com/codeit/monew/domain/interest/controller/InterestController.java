package com.codeit.monew.domain.interest.controller;

import com.codeit.monew.domain.interest.dto.request.InterestRegisterRequest;
import com.codeit.monew.domain.interest.dto.request.InterestUpdateRequest;
import com.codeit.monew.domain.interest.dto.response.CursorPageResponseInterestDto;
import com.codeit.monew.domain.interest.dto.response.InterestDto;
import com.codeit.monew.domain.interest.dto.response.SubscriptionDto;
import com.codeit.monew.domain.interest.service.InterestService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class InterestController {

  private final InterestService interestService;

  // 1. 관심사 등록
  @PostMapping
  public ResponseEntity<InterestDto> register(
      @RequestBody @Valid InterestRegisterRequest request
  ) {
    InterestDto response = interestService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  // 2. 관심사 수정
  @PatchMapping("/{interestId}")
  public ResponseEntity<InterestDto> update(
      @PathVariable UUID interestId,
      @RequestBody @Valid InterestUpdateRequest request
  ) {
    InterestDto response = interestService.update(interestId, request);
    return ResponseEntity.ok(response);
  }

  // 3. 관심사 삭제
  @DeleteMapping("/{interestId}")
  public ResponseEntity<Void> delete(
      @PathVariable UUID interestId
  ) {
    interestService.delete(interestId);
    return ResponseEntity.noContent().build();
  }

  // 4. 관심사 목록 조회

  // 5. 관심사 구독
  @PostMapping("/{interestId}/subscriptions")
  public ResponseEntity<SubscriptionDto> subscribe(
      @PathVariable UUID interestId,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    SubscriptionDto response = interestService.subscribe(interestId, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  // 6. 관심사 구독 취소
  @DeleteMapping("/{interestId}/subscriptions")
  public ResponseEntity<Void> unsubscribe(
      @PathVariable UUID interestId,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    interestService.unsubscribe(interestId, userId);
    return ResponseEntity.noContent().build();
  }

}
