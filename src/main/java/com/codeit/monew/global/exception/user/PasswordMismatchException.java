package com.codeit.monew.global.exception.user;

import com.codeit.monew.global.exception.ErrorCode;

public class PasswordMismatchException extends UserException {

  public PasswordMismatchException(String email) {
    super(ErrorCode.PASSWORD_MISMATCH, "email", email);
  }
}
