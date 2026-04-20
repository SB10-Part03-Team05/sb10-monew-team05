package com.codeit.monew.global.exception.Interest;

import com.codeit.monew.global.exception.ErrorCode;
import java.util.UUID;

public class SubscriptionNotFoundException extends InterestException {

  public SubscriptionNotFoundException(UUID userId, UUID interestId) {

    super(ErrorCode.SUBSCRIPTION_NOT_FOUND, "userId", userId);
    addDetail("interestId", interestId);
  }
}
