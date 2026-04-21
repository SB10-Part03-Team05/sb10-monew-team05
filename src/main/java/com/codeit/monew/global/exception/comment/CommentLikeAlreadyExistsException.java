package com.codeit.monew.global.exception.comment;

import com.codeit.monew.global.exception.ErrorCode;
import java.util.UUID;

public class CommentLikeAlreadyExistsException extends CommentException {

  public CommentLikeAlreadyExistsException(UUID commentId, UUID userId) {
    super(ErrorCode.COMMENT_LIKE_ALREADY_EXISTS, "commentId & userId", commentId + " & " + userId);
  }
}
