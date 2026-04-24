package com.codeit.monew.global.exception.notification;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.common.CommonException;
import java.util.UUID;

public class NotificationAccessDeniedException extends CommonException {

  public NotificationAccessDeniedException(UUID notificationId, UUID requestUserId) {
    super(ErrorCode.NOTIFICATION_ACCESS_DENIED, "notificationId", notificationId, "requestUserId", requestUserId);
  }
}
