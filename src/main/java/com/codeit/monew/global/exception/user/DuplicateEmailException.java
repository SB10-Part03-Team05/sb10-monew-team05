package com.codeit.monew.global.exception.user;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.MonewException;

public class DuplicateEmailException extends UserException {

  public DuplicateEmailException(String email) {
    super(ErrorCode.DUPLICATE_EMAIL, "email", email);
  }
}
