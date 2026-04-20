package com.codeit.monew.domain.comment.controller;

import com.codeit.monew.domain.comment.dto.CommentLikeDto;
import com.codeit.monew.domain.comment.service.CommentLikeService;
import io.swagger.v3.oas.annotations.Operation;
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
  @PostMapping
  public ResponseEntity<CommentLikeDto> addLike(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID requesterId) {

    log.debug("댓글 좋아요 등록 요청: commentId={}, requesterId={}", commentId, requesterId);
    CommentLikeDto response = commentLikeService.addLike(commentId, requesterId);

    return ResponseEntity.ok(response);
  }

  @Operation(summary = "댓글 좋아요 취소", description = "댓글 좋아요를 취소합니다.")
  @DeleteMapping
  public ResponseEntity<Void> cancelLike(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID requesterId) {

    log.debug("댓글 좋아요 취소 요청: commentId={}, requesterId={}", commentId, requesterId);
    commentLikeService.cancelLike(commentId, requesterId);

    return ResponseEntity.ok().build();
  }
}
