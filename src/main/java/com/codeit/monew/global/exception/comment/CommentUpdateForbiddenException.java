package com.codeit.monew.global.exception.comment;

import com.codeit.monew.global.exception.ErrorCode;
import java.util.UUID;

public class CommentUpdateForbiddenException extends CommentException {

  public CommentUpdateForbiddenException(UUID requesterId) {
    super(ErrorCode.COMMENT_UPDATE_FORBIDDEN, "requesterId", requesterId);
  }
}
