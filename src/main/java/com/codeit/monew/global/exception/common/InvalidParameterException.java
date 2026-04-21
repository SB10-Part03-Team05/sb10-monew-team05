package com.codeit.monew.global.exception.common;

import com.codeit.monew.global.exception.ErrorCode;

public class InvalidParameterException extends CommonException {

  public InvalidParameterException(String field, Object parameter) {
    super(ErrorCode.INVALID_PARAMETER_INPUT, field, parameter);
  }

  public InvalidParameterException(String field1, Object parameter1, String field2,
      Object parameter2) {
    super(ErrorCode.INVALID_PARAMETER_INPUT, field1, parameter1, field2, parameter2);
  }

}
