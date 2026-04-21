package com.codeit.monew.global.exception.comment;

import com.codeit.monew.global.exception.ErrorCode;
import java.util.UUID;

public class CommentLikeNotFoundException extends CommentException {

  public CommentLikeNotFoundException(UUID commentId, UUID userId) {
    super(ErrorCode.COMMENT_LIKE_NOT_FOUND, "commentId & userId", commentId + " & " + userId);
  }
}
