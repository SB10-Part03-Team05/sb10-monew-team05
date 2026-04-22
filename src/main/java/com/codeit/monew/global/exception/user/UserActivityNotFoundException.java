package com.codeit.monew.global.exception.user;

import com.codeit.monew.global.exception.ErrorCode;
import java.util.UUID;

public class UserActivityNotFoundException extends UserException {

  public UserActivityNotFoundException(UUID userId) {
    super(ErrorCode.USER_NOT_FOUND, "userId", userId);
  }
}
