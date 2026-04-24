package com.codeit.monew.global.exception.notification;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.common.CommonException;
import java.util.UUID;

public class NotificationNotFoundException extends CommonException {

  public NotificationNotFoundException(UUID notificationId) {
    super(ErrorCode.NOTIFICATION_NOT_FOUND, "notificationId", notificationId);
  }
}
