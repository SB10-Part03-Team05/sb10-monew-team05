package com.codeit.monew.domain.comment.controller;

import com.codeit.monew.domain.comment.dto.CommentCursorRequest;
import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.comment.dto.CommentRegisterRequest;
import com.codeit.monew.domain.comment.dto.CommentUpdateRequest;
import com.codeit.monew.domain.comment.dto.CursorPageResponseCommentDto;
import com.codeit.monew.domain.comment.service.CommentService;
import com.codeit.monew.global.exception.comment.CommentNotFoundException;
import com.codeit.monew.global.exception.comment.CommentUpdateForbiddenException;
import com.codeit.monew.global.exception.user.UserNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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

  @Nested
  @DisplayName("댓글 수정 API 테스트")
  class UpdateCommentAPI {

    private final String URL = "/api/comments/{commentId}";
    private final String HEADER_NAME = "Monew-Request-User-ID";

    @Test
    @DisplayName("올바른 요청을 보내면 200 OK와 수정된 데이터를 반환한다.")
    void success_update_comment() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      CommentUpdateRequest request = new CommentUpdateRequest("수정할 내용");

      CommentDto responseDto = new CommentDto(commentId, UUID.randomUUID(), userId, "닉네임", "수정할 내용", 0L, false, null);
      given(commentService.updateComment(eq(commentId), eq(userId), any())).willReturn(responseDto);

      // when & then
      mockMvc.perform(patch(URL, commentId)
              .header(HEADER_NAME, userId.toString())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").value("수정할 내용"));
    }

    @Test
    @DisplayName("필수 헤더(Monew-Request-User-ID)가 없으면 400 Bad Request를 반환한다.")
    void fail_missingHeader() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      CommentUpdateRequest request = new CommentUpdateRequest("내용");

      // when & then (헤더 누락)
      mockMvc.perform(patch(URL, commentId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("MISSING_REQUEST_HEADER"));
    }

    @Test
    @DisplayName("내용이 비어있으면 @Valid 검증에 걸려 400 Bad Request를 반환한다.")
    void fail_invalidRequestBody() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      CommentUpdateRequest request = new CommentUpdateRequest(""); // 빈 문자열 (Validation 에러 유도)

      // when & then
      mockMvc.perform(patch(URL, commentId)
              .header(HEADER_NAME, userId.toString())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("존재하지 않는 댓글 ID면 404 Not Found를 반환한다.")
    void fail_commentNotFound() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      CommentUpdateRequest request = new CommentUpdateRequest("내용");

      given(commentService.updateComment(any(), any(), any()))
          .willThrow(new CommentNotFoundException(commentId));

      // when & then
      mockMvc.perform(patch(URL, commentId)
              .header(HEADER_NAME, userId.toString())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("작성자가 아닌 사람이 수정하면 403 Forbidden을 반환한다.")
    void fail_updateForbidden() throws Exception {
      // given
      UUID commentId = UUID.randomUUID(); // 작성자
      UUID hackerId = UUID.randomUUID(); // 요청자
      CommentUpdateRequest request = new CommentUpdateRequest("작성자가 아닌 유저가 댓글 수정 시도");

      given(commentService.updateComment(any(), any(), any()))
          .willThrow(new CommentUpdateForbiddenException(hackerId));

      // when & then
      mockMvc.perform(patch(URL, commentId)
              .header(HEADER_NAME, hackerId.toString())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("COMMENT_UPDATE_FORBIDDEN"));
    }
  }

  @Nested
  @DisplayName("댓글 논리 삭제 API 테스트")
  class DeleteCommentAPI {

    private final String URL = "/api/comments/{commentId}";

    @Test
    @DisplayName("정상적인 삭제 요청 시 204 No Content를 반환한다.")
    void success_logical_delete() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      // 삭제 메서드는 반환값이 void이므로 willDoNothing() 사용 (생략 가능)

      // when & then
      mockMvc.perform(delete(URL, commentId))
          .andDo(print())
          .andExpect(status().isNoContent()); // 💡 204 상태 코드 검증

      verify(commentService).deleteComment(commentId);
    }

    @Test
    @DisplayName("존재하지 않는 댓글 ID로 논리 삭제 요청 시 404 Not Found를 반환한다.")
    void fail_logical_delete_notFound() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();

      doThrow(new CommentNotFoundException(commentId))
          .when(commentService).deleteComment(commentId);

      // when & then
      mockMvc.perform(delete(URL, commentId))
          .andDo(print())
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("댓글 물리 삭제 API 테스트")
  class HardDeleteCommentAPI {

    private final String URL = "/api/comments/{commentId}/hard";

    @Test
    @DisplayName("정상적인 물리 삭제 요청 시 204 No Content를 반환한다.")
    void success_hard_delete() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();

      // when & then
      mockMvc.perform(delete(URL, commentId))
          .andDo(print())
          .andExpect(status().isNoContent());

      verify(commentService).hardDeleteComment(commentId);
    }

    @Test
    @DisplayName("존재하지 않는 댓글 ID로 물리 삭제 요청 시 404 Not Found를 반환한다.")
    void fail_hard_delete_notFound() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();

      doThrow(new CommentNotFoundException(commentId))
          .when(commentService).hardDeleteComment(commentId);

      // when & then
      mockMvc.perform(delete(URL, commentId))
          .andDo(print())
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("댓글 목록 조회 API 테스트")
  class GetCommentListApiTest {

    @Test
    @DisplayName("올바른 파라미터로 요청하면 200 OK와 함께 페이징 데이터를 반환한다.")
    void success_getCommentList() throws Exception {
      // given
      UUID articleId = UUID.randomUUID();
      UUID requesterId = UUID.randomUUID();
      Instant now = Instant.now();

      CommentDto mockComment = new CommentDto(
          UUID.randomUUID(), articleId, UUID.randomUUID(), "테스트 닉네임",
          "테스트 댓글", 10L, true, now
      );

      CursorPageResponseCommentDto responseDto = new CursorPageResponseCommentDto(
          List.of(mockComment), "10", now, 1, 15L, true
      );

      given(commentService.getCommentList(eq(articleId), eq(requesterId), any(CommentCursorRequest.class)))
          .willReturn(responseDto);

      // when & then
      mockMvc.perform(get("/api/comments")
              .header("Monew-Request-User-ID", requesterId.toString())
              .param("articleId", articleId.toString())
              .param("orderBy", "likeCount")
              .param("direction", "DESC")
              .param("limit", "10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].content").value("테스트 댓글"))
          .andExpect(jsonPath("$.nextCursor").value("10"))
          .andExpect(jsonPath("$.hasNext").value(true))
          .andExpect(jsonPath("$.totalElements").value(15));
    }

    @Test
    @DisplayName("limit가 최대 허용치(100)를 초과하면 400 Bad Request를 반환한다.")
    void fail_limit_exceeded() throws Exception {
      // given
      UUID articleId = UUID.randomUUID();
      UUID requesterId = UUID.randomUUID();

      // when & then
      mockMvc.perform(get("/api/comments")
              .header("Monew-Request-User-ID", requesterId.toString())
              .param("articleId", articleId.toString())
              .param("limit", "101"))
          .andExpect(status().isBadRequest());

      verify(commentService, never()).getCommentList(any(), any(), any());
    }

    @Test
    @DisplayName("올바르지 않은 정렬 기준(orderBy)이 들어오면 400 Bad Request를 반환한다.")
    void fail_invalid_orderBy() throws Exception {
      // given
      UUID articleId = UUID.randomUUID();
      UUID requesterId = UUID.randomUUID();

      // when & then
      mockMvc.perform(get("/api/comments")
              .header("Monew-Request-User-ID", requesterId.toString())
              .param("articleId", articleId.toString())
              .param("orderBy", "weirdCondition"))
          .andExpect(status().isBadRequest());

      verify(commentService, never()).getCommentList(any(), any(), any());
    }

    @Test
    @DisplayName("필수 파라미터인 articleId가 누락되면 400 Bad Request를 반환한다.")
    void fail_missing_articleId() throws Exception {
      // given
      UUID requesterId = UUID.randomUUID();

      // when & then
      mockMvc.perform(get("/api/comments")
              .header("Monew-Request-User-ID", requesterId.toString())
              .param("limit", "10"))
          .andExpect(status().isBadRequest());

      verify(commentService, never()).getCommentList(any(), any(), any());
    }
  }
}