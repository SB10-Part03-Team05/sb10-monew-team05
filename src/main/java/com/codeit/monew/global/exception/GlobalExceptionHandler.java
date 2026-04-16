package com.codeit.monew.global.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e) {
    log.error("예상치 못한 오류 발생: {}", e.getMessage(), e);
    ErrorResponse errorResponse = new ErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR.value());
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(errorResponse);
  }

  @ExceptionHandler(MonewException.class)
  public ResponseEntity<ErrorResponse> handleMonewException(MonewException exception) {
    log.error("커스텀 예외 발생: code={}, message={}", exception.getErrorCode(), exception.getMessage(),
        exception);
    HttpStatus status = determineHttpStatus(exception);
    ErrorResponse response = new ErrorResponse(exception, status.value());
    return ResponseEntity
        .status(status)
        .body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    log.error("요청 유효성 검사 실패: {}", ex.getMessage());

    Map<String, Object> validationErrors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      validationErrors.put(fieldName, errorMessage);
    });

    ErrorResponse response = new ErrorResponse(
        Instant.now(),
        "VALIDATION_ERROR",
        "요청 데이터 유효성 검사에 실패했습니다",
        validationErrors,
        ex.getClass().getSimpleName(),
        HttpStatus.BAD_REQUEST.value()
    );

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(response);
  }

  private HttpStatus determineHttpStatus(MonewException exception) {
    ErrorCode errorCode = exception.getErrorCode();
    return switch (errorCode) {
//            case  -> HttpStatus.CONFLICT;
//            case  -> HttpStatus.UNAUTHORIZED;
//            case  -> HttpStatus.BAD_REQUEST;
//            case  -> HttpStatus.INTERNAL_SERVER_ERROR;

      case USER_NOT_FOUND, ARTICLE_NOT_FOUND -> HttpStatus.NOT_FOUND;
      case DUPLICATE_EMAIL -> HttpStatus.CONFLICT;
      case COMMENT_UPDATE_FORBIDDEN -> HttpStatus.FORBIDDEN; // 댓글 수정 권한 없음 -> 403 Forbidden

      default -> HttpStatus.INTERNAL_SERVER_ERROR; // 지금은 디버깅 에러 잡는 용도, 나중에 지워야 함
    };
  }
}
