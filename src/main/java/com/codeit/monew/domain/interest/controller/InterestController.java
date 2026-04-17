package com.codeit.monew.domain.interest.controller;

import com.codeit.monew.domain.interest.dto.request.InterestRegisterRequest;
import com.codeit.monew.domain.interest.dto.response.InterestDto;
import com.codeit.monew.domain.interest.service.InterestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

}
