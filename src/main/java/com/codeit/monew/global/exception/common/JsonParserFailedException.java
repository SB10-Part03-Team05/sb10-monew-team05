package com.codeit.monew.global.exception.common;

import com.codeit.monew.global.exception.ErrorCode;

public class JsonParserFailedException extends CommonException {

  public JsonParserFailedException(Throwable cause) {
    super(ErrorCode.JSON_PARSER_FAILED, "message", "JSON Parser Failed", cause);
  }
}
