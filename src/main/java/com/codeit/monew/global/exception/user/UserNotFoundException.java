package com.codeit.monew.global.exception.user;

import com.codeit.monew.global.exception.ErrorCode;
import java.util.UUID;

public class UserNotFoundException extends UserException {

  public UserNotFoundException(UUID userId) {
    super(ErrorCode.USER_NOT_FOUND, "userId", userId);
  }

}
