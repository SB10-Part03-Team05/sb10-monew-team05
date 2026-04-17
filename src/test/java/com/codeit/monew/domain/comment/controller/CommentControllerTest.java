package com.codeit.monew.domain.comment.controller;

import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.comment.dto.CommentRegisterRequest;
import com.codeit.monew.domain.comment.service.CommentService;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private CommentService commentService;

  @Nested
  @DisplayName("댓글 등록 API 테스트")
  class RegisterComment {

    @Test
    @DisplayName("모든 입력값이 정상이면 댓글이 등록되고 201 Created가 반환된다.")
    void success_register_comment() throws Exception {
      // given
      UUID articleId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      CommentRegisterRequest request = new CommentRegisterRequest(articleId, userId, "테스트 댓글");
      CommentDto response = new CommentDto(UUID.randomUUID(), articleId, userId, "닉네임", "테스트 댓글", 0L, false, Instant.now());

      given(commentService.registerComment(any(), any(), any())).willReturn(response);

      // when & then
      mockMvc.perform(post("/api/comments")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.content").value("테스트 댓글"));
    }

    @Test
    @DisplayName("댓글 내용이 비어있으면 400 Bad Request을 반환한다.")
    void fail_comment_blankContent() throws Exception {
      // given
      CommentRegisterRequest request = new CommentRegisterRequest(UUID.randomUUID(), UUID.randomUUID(), "");

      // when & then
      mockMvc.perform(post("/api/comments")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 유저면 404 Not Found을 반환한다.")
    void fail_comment_userNotFound() throws Exception {
      // given
      CommentRegisterRequest request = new CommentRegisterRequest(UUID.randomUUID(), UUID.randomUUID(), "내용");

      // 서비스가 UserNotFoundException을 던지도록 설정
      given(commentService.registerComment(any(), any(), any()))
          .willThrow(new UserNotFoundException(request.userId()));

      // when & then
      mockMvc.perform(post("/api/comments")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound()); // GlobalExceptionHandler가 404로 변환해줌
    }
  }
}