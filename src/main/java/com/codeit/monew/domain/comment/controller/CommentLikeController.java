package com.codeit.monew.domain.comment.controller;

import com.codeit.monew.domain.comment.dto.CommentLikeDto;
import com.codeit.monew.domain.comment.service.CommentLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "댓글 관리", description = "댓글 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/comments/{commentId}/comment-likes")
@RequiredArgsConstructor
public class CommentLikeController {
  private final CommentLikeService commentLikeService;

  @Operation(summary = "댓글 좋아요 등록", description = "댓글에 좋아요를 등록합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "댓글 좋아요 성공"),
      @ApiResponse(responseCode = "404", description = "댓글 또는 사용자 정보 없음"),
      @ApiResponse(responseCode = "409", description = "이미 좋아요를 누른 상태 (중복 요청)"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @PostMapping
  public ResponseEntity<CommentLikeDto> addLike(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID requesterId) {

    log.debug("댓글 좋아요 등록 요청: commentId={}, requesterId={}", commentId, requesterId);
    CommentLikeDto response = commentLikeService.addLike(commentId, requesterId);

    return ResponseEntity.ok(response);
  }

  @Operation(summary = "댓글 좋아요 취소", description = "댓글 좋아요를 취소합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "댓글 좋아요 취소 성공"),
      @ApiResponse(responseCode = "404", description = "댓글 또는 취소할 좋아요 정보 없음"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @DeleteMapping
  public ResponseEntity<Void> cancelLike(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID requesterId) {

    log.debug("댓글 좋아요 취소 요청: commentId={}, requesterId={}", commentId, requesterId);
    commentLikeService.cancelLike(commentId, requesterId);

    return ResponseEntity.ok().build();
  }
}
