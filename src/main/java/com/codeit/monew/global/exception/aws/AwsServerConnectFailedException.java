package com.codeit.monew.global.exception.aws;

import com.codeit.monew.global.exception.ErrorCode;

public class AwsServerConnectFailedException extends AwsException {

  public AwsServerConnectFailedException(Throwable cause) {
    super(ErrorCode.AWS_SERVER_CONNECT_FAILED, "message", "AWS Server Connect Failed", cause);
  }
}
