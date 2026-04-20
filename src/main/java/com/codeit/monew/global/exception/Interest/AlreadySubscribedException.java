package com.codeit.monew.global.exception.Interest;

import com.codeit.monew.global.exception.ErrorCode;
import java.util.UUID;

public class AlreadySubscribedException extends InterestException {

  public AlreadySubscribedException(UUID userId, UUID interestId) {

    super(ErrorCode.ALREADY_SUBSCRIBED, "userId", userId);
    addDetail("interestId", interestId);
  }
}
