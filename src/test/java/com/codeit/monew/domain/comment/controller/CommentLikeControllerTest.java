package com.codeit.monew.domain.comment.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.monew.domain.comment.dto.CommentLikeDto;
import com.codeit.monew.domain.comment.service.CommentLikeService;
import com.codeit.monew.global.exception.comment.CommentLikeAlreadyExistsException;
import com.codeit.monew.global.exception.comment.CommentLikeNotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommentLikeController.class)
class CommentLikeControllerTest {
  @Autowired private MockMvc mockMvc;

  @MockitoBean private CommentLikeService commentLikeService;

  @Nested
  @DisplayName("댓글 좋아요 등록 API 테스트")
  class AddLikeApiTest {

    @Test
    @DisplayName("좋아요 등록 성공 시 200 OK와 CommentLikeDto를 반환한다.")
    void success_addLike() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID requesterId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();

      CommentLikeDto responseDto = new CommentLikeDto(
          UUID.randomUUID(), requesterId, Instant.now(), commentId, articleId,
          UUID.randomUUID(), "testUser", "댓글 내용", 1L, Instant.now()
      );

      given(commentLikeService.addLike(eq(commentId), eq(requesterId))).willReturn(responseDto);

      // when & then
      mockMvc.perform(post("/api/comments/{commentId}/comment-likes", commentId)
              .header("Monew-Request-User-ID", requesterId.toString()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.commentId").value(commentId.toString()))
          .andExpect(jsonPath("$.likedBy").value(requesterId.toString()))
          .andExpect(jsonPath("$.commentLikeCount").value(1));
    }

    @Test
    @DisplayName("이미 좋아요를 누른 댓글에 또 좋아요를 누른다면, 409 Conflict를 반환한다")
    void fail_alreadyExists() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID requesterId = UUID.randomUUID();

      given(commentLikeService.addLike(eq(commentId), eq(requesterId)))
          .willThrow(new CommentLikeAlreadyExistsException(commentId, requesterId));

      // when & then
      mockMvc.perform(post("/api/comments/{commentId}/comment-likes", commentId)
              .header("Monew-Request-User-ID", requesterId.toString()))
          .andExpect(status().isConflict());
    }
  }

  @Nested
  @DisplayName("댓글 좋아요 취소 API 테스트")
  class CancelLikeApiTest {

    @Test
    @DisplayName("좋아요 취소 시 200 OK 빈 바디를 반환한다.")
    void success() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID requesterId = UUID.randomUUID();

      doNothing().when(commentLikeService).cancelLike(eq(commentId), eq(requesterId));

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", commentId)
              .header("Monew-Request-User-ID", requesterId.toString()))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("취소할 좋아요가 없는 경우 404 Not Found를 반환한다.")
    void fail_notFound() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID requesterId = UUID.randomUUID();

      doThrow(new CommentLikeNotFoundException(commentId, requesterId))
          .when(commentLikeService).cancelLike(eq(commentId), eq(requesterId));

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", commentId)
              .header("Monew-Request-User-ID", requesterId.toString()))
          .andExpect(status().isNotFound());
    }
  }
}