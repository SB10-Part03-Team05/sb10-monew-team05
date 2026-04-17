package com.codeit.monew.global.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.View;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private final View error;

  public GlobalExceptionHandler(View error) {
    this.error = error;
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e) {
    log.error("[Exception] 예상하지 못한 예외: code={}, message={}", e.getClass().getSimpleName(),
        e.getMessage(), e);
    ErrorResponse errorResponse = new ErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR.value());
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(errorResponse);
  }

  @ExceptionHandler(MonewException.class)
  public ResponseEntity<ErrorResponse> handleMonewException(MonewException e) {
    HttpStatus status = determineHttpStatus(e);

    if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
      log.error("[Exception] 커스텀 예외: timestamp={}, code={}, message={}, details={}",
          e.getTimestamp(), e.getErrorCode().name(), e.getMessage(), e.getDetails(), e);
    } else {
      log.warn("[Exception] 커스텀 예외: timestamp={}, code={}, message={}, details={}",
          e.getTimestamp(), e.getErrorCode().name(), e.getMessage(), e.getDetails(), e);
    }

    ErrorResponse response = new ErrorResponse(e, status.value());
    return ResponseEntity
        .status(status)
        .body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException e) {
    log.warn("[EXCEPTION] Bean Validation 예외: code={}, message={}", e.getClass().getSimpleName(),
        e.getMessage(), e);

    Map<String, Object> details = new HashMap<>();
    e.getBindingResult().getFieldErrors().forEach(fieldError -> {
      details.put(fieldError.getField(), fieldError.getDefaultMessage());
    });
    e.getBindingResult().getGlobalErrors().forEach(fieldError -> {
      details.put(fieldError.getObjectName(), fieldError.getDefaultMessage());
    });

    ErrorResponse response = new ErrorResponse(
        Instant.now(),
        "VALIDATION_ERROR",
        "요청 데이터 유효성 검사에 실패했습니다",
        details,
        e.getClass().getSimpleName(),
        HttpStatus.BAD_REQUEST.value()
    );

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(response);
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponse> handleException(MissingRequestHeaderException e) {
    log.warn("[EXCEPTION] 필수 헤더 누락 예외: code={}, header={}, message={}",
        e.getClass().getSimpleName(), e.getHeaderName(), e.getMessage());

    Map<String, Object> details = new HashMap<>();
    details.put("headerName", e.getHeaderName());

    ErrorResponse errorResponse = new ErrorResponse(
        Instant.now(),
        "MISSING_REQUEST_HEADER",
        "필수 요청 헤더가 누락되었습니다.",
        details,
        e.getClass().getSimpleName(),
        HttpStatus.BAD_REQUEST.value()
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleException(MethodArgumentTypeMismatchException e) {
    log.warn("[EXCEPTION] 요청 파라미터 형식 예외: code={}, message={}", e.getClass().getSimpleName(),
        e.getMessage(), e);

    Map<String, Object> details = new HashMap<>();
    details.put(e.getName(), e.getValue()); // (파라미터 필드 이름, 파라미터 필드 값)

    ErrorResponse errorResponse = new ErrorResponse(
        Instant.now(),
        "INVALID_PARAMETER_TYPE",
        "요청 파라미터 형식이 올바르지 않습니다.",
        details,
        e.getClass().getSimpleName(),
        HttpStatus.BAD_REQUEST.value()
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
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
      case USER_ACCESS_DENIED, COMMENT_UPDATE_FORBIDDEN -> HttpStatus.FORBIDDEN;
      case COMMENT_CONTENT_BLANK, COMMENT_CONTENT_TOO_LONG -> HttpStatus.BAD_REQUEST;

      default -> HttpStatus.INTERNAL_SERVER_ERROR; // 지금은 디버깅 에러 잡는 용도, 나중에 지워야 함
    };
  }
}
