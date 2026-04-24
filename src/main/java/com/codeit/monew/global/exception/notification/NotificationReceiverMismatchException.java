package com.codeit.monew.global.exception.notification;

import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.common.CommonException;
import java.util.UUID;

public class NotificationReceiverMismatchException extends CommonException {

  public NotificationReceiverMismatchException(UUID commentId, UUID readerId) {
    super(ErrorCode.NOTIFICATION_RECEIVER_MISMATCH, "commentId", commentId, "readerId", readerId);
  }
}
