package com.codeit.monew.global.exception.interest;

import com.codeit.monew.global.exception.ErrorCode;
import java.util.UUID;

public class InterestNotFoundException extends InterestException {

  public InterestNotFoundException(UUID interestId) {
    super(ErrorCode.INTEREST_NOT_FOUND, "interestId", interestId);
  }
}
