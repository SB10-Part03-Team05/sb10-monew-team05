package com.codeit.monew.global.exception.comment;

import com.codeit.monew.global.exception.ErrorCode;
import java.util.UUID;

public class CommentNotFoundException extends CommentException {

  public CommentNotFoundException(UUID commentId) {
    super(ErrorCode.COMMENT_NOT_FOUND, "commentId", commentId);
  }
}