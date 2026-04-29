package com.codeit.monew.global.exception;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
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

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
    log.warn("[EXCEPTION] Constraint Violation 예외: code={}, message={}", e.getClass().getSimpleName(),
        e.getMessage(), e);
    Map<String, Object> details = new HashMap<>();
    e.getConstraintViolations().forEach(violation -> {
      String propertyPath = violation.getPropertyPath().toString();
      String fieldName = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);

      details.put(fieldName, violation.getMessage());
    });

    ErrorResponse errorResponse = new ErrorResponse(
        Instant.now(),
        "CONSTRAINT_VIOLATION_ERROR",
        "요청 파라미터 유효성 검사에 실패했습니다.",
        details,
        e.getClass().getSimpleName(),
        HttpStatus.BAD_REQUEST.value()
    );

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(errorResponse);
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

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleException(MissingServletRequestParameterException e) {
    log.warn("[EXCEPTION] 필수 요청 파라미터 누락 예외: code={}, message={}", e.getClass().getSimpleName(),
        e.getMessage(), e);

    Map<String, Object> details = new HashMap<>();
    details.put("parameterName", e.getParameterName());

    ErrorResponse errorResponse = new ErrorResponse(
        Instant.now(),
        "MISSING_REQUEST_PARAMETER",
        "필수 파라미터가 누락되었습니다.",
        details,
        e.getClass().getSimpleName(),
        HttpStatus.BAD_REQUEST.value()
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
    log.warn("[EXCEPTION] 잘못된 요청: message={}", e.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(e, HttpStatus.BAD_REQUEST.value());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException e) {
    log.warn("[EXCEPTION] JSON 역직렬화 실패: message={}", e.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(e, HttpStatus.BAD_REQUEST.value());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler({InvalidDataAccessResourceUsageException.class, DataAccessException.class})
  public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException e) {
    log.error("[EXCEPTION] 데이터베이스 오류: exceptionType={}", e.getClass().getSimpleName());

    ErrorResponse errorResponse = new ErrorResponse(
        Instant.now(),
        "DATABASE_ERROR",
        "서버 내부 데이터베이스 오류가 발생했습니다.",
        new HashMap<>(), // 쿼리 정보가 노출되면 안 되므로 빈 Map 전달
        e.getClass().getSimpleName(),
        HttpStatus.INTERNAL_SERVER_ERROR.value()
    );

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  private HttpStatus determineHttpStatus(MonewException exception) {
    ErrorCode errorCode = exception.getErrorCode();
    return switch (errorCode) {
      case USER_NOT_FOUND, ARTICLE_NOT_FOUND, COMMENT_NOT_FOUND, INTEREST_NOT_FOUND,
           SUBSCRIPTION_NOT_FOUND, COMMENT_LIKE_NOT_FOUND, NOTIFICATION_NOT_FOUND ->
          HttpStatus.NOT_FOUND; // 404
      case PASSWORD_MISMATCH -> HttpStatus.UNAUTHORIZED; // 401
      case DUPLICATE_EMAIL, DUPLICATE_INTEREST, ALREADY_SUBSCRIBED, COMMENT_LIKE_ALREADY_EXISTS ->
          HttpStatus.CONFLICT; // 409
      case USER_ACCESS_DENIED, COMMENT_UPDATE_FORBIDDEN, NOTIFICATION_ACCESS_DENIED,
           NOTIFICATION_RECEIVER_MISMATCH -> HttpStatus.FORBIDDEN; // 403
      case INVALID_PARAMETER_INPUT, COMMENT_CONTENT_BLANK, COMMENT_CONTENT_TOO_LONG,
           INVALID_ARTICLE_ENTITY -> HttpStatus.BAD_REQUEST; // 400
      case EXTERNAL_RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS; // 429
      case EXTERNAL_CLIENT_ERROR -> HttpStatus.BAD_GATEWAY; // 502
      case EXTERNAL_SERVER_ERROR, EXTERNAL_EMPTY_RESPONSE, EXTERNAL_NETWORK_ERROR,
           EXTERNAL_INVALID_XML, AWS_SERVER_CONNECT_FAILED, ARTICLE_FILE_SAVE_FAILED,
           ARTICLE_FILE_READ_FAILED -> HttpStatus.SERVICE_UNAVAILABLE; // 503
      case ARTICLE_SCRAPE_FAILED, JSON_PARSER_FAILED, ARTICLE_BACKUP_BATCH_RUN_FAILED ->
          HttpStatus.INTERNAL_SERVER_ERROR; // 500, 커스텀 예외
      default -> HttpStatus.INTERNAL_SERVER_ERROR; // 500, 알수 없는 에러
    };
  }
}
