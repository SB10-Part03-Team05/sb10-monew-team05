package com.codeit.monew.domain.comment.repository;

import com.codeit.monew.domain.comment.dto.CommentCursorRequest;
import com.codeit.monew.domain.comment.entity.Comment;
import java.util.List;
import java.util.UUID;

public interface CommentQueryRepository {

  // 동적 정렬 및 커서 페이징을 적용한 댓글 목록 조회
  List<Comment> findCommentsByCursor(UUID articleId, CommentCursorRequest request);
}
