package com.codeit.monew.domain.comment.controller;

import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.comment.dto.CommentRegisterRequest;
import com.codeit.monew.domain.comment.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "댓글 기사 관리", description = "댓글 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;

  @Operation(summary = "댓글 등록", description = "새로운 댓글을 등록합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "등록 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @PostMapping
  public ResponseEntity<CommentDto> registerComment(
      @Valid @RequestBody CommentRegisterRequest request) {

    log.debug("댓글 등록 요청: 요청자 userId= {}", request.userId());

    CommentDto response = commentService.registerComment(
        request.articleId(),
        request.userId(),
        request.content()
    );

    log.info("댓글 등록 성공: 생성된 commentId= {}", response.id());

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
