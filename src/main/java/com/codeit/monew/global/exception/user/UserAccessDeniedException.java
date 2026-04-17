package com.codeit.monew.global.exception.user;

import com.codeit.monew.global.exception.ErrorCode;
import java.util.UUID;

public class UserAccessDeniedException extends UserException {

  public UserAccessDeniedException(UUID requesterId) {
    super(ErrorCode.USER_ACCESS_DENIED, "requesterId", requesterId);
  }
}
