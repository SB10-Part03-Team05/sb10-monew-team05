package com.codeit.monew.global.exception.Interest;

import com.codeit.monew.global.exception.ErrorCode;

public class DuplicateInterestException extends InterestException {

  public DuplicateInterestException(String name) {

    super(ErrorCode.DUPLICATE_INTEREST, "name", name);
  }
}
