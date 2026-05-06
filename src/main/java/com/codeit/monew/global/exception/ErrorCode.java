package com.codeit.monew.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  // common
  INVALID_PARAMETER_INPUT(HttpStatus.BAD_REQUEST, "입력값이 유효하지 않습니다."),
  JSON_PARSER_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "JSON 직렬화/역직렬화에 실패했습니다."),

  // User 관련 에러 코드
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
  DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용중인 이메일입니다."),
  USER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "사용자 수정, 삭제 권한이 없습니다."),
  PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),

  // Article 관련 에러 코드
  ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "뉴스 기사 정보를 찾을 수 없습니다."),
  INVALID_ARTICLE_ENTITY(HttpStatus.BAD_REQUEST, "뉴스 기사 정보가 유효하지 않습니다."),
  ARTICLE_SCRAPE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "뉴스 기사 수집 처리 중 오류가 발생했습니다."),
  ARTICLE_BACKUP_BATCH_RUN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "뉴스 기사 배치 실행에 실패했습니다."),
  ARTICLE_FILE_SAVE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "뉴스 기사 파일 저장에 실패했습니다."),
  ARTICLE_FILE_READ_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "뉴스 기사 파일 읽기에 실패했습니다."),

  // Interest 관련 에러 코드
  INTEREST_NOT_FOUND(HttpStatus.NOT_FOUND, "관심사를 찾을 수 없습니다."),
  DUPLICATE_INTEREST(HttpStatus.CONFLICT, "유사한 관심사가 이미 존재합니다."),
  ALREADY_SUBSCRIBED(HttpStatus.CONFLICT, "이미 구독 중인 관심사입니다."),
  SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "구독 정보를 찾을 수 없습니다."),

  // Comment 관련 에러 코드
  COMMENT_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "댓글 수정 권한이 없습니다."),
  COMMENT_CONTENT_BLANK(HttpStatus.BAD_REQUEST, "댓글 내용은 비어있을 수 없습니다."),
  COMMENT_CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "댓글 내용은 500자를 초과할 수 없습니다."),
  COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글 정보를 찾을 수 없습니다."),
  COMMENT_LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 좋아요를 누른 댓글입니다."),
  COMMENT_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "좋아요 정보가 없습니다."),

  // Notification 관련 에러 코드
  NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림 정보를 찾을 수 없습니다."),
  NOTIFICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 알림에 접근할 권한이 없습니다."),
  NOTIFICATION_RECEIVER_MISMATCH(HttpStatus.FORBIDDEN, "댓글 작성자와 알림 수신자가 일치하지 않습니다."),

  // Server 에러 코드
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

  // External 관련 에러 코드
  EXTERNAL_CLIENT_ERROR(HttpStatus.BAD_GATEWAY, "외부 연동 요청 오류가 발생했습니다."),
  EXTERNAL_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "외부 연동 요청 제한이 발생했습니다."),
  EXTERNAL_SERVER_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "외부 연동 서버 오류가 발생했습니다."),
  EXTERNAL_EMPTY_RESPONSE(HttpStatus.SERVICE_UNAVAILABLE, "외부 연동 응답 본문이 비어 있습니다."),
  EXTERNAL_NETWORK_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "외부 연동 네트워크 오류가 발생했습니다."),
  EXTERNAL_INVALID_XML(HttpStatus.SERVICE_UNAVAILABLE, "외부 연동 XML 파싱에 실패했습니다."),
  EMPTY_XML_INPUT(HttpStatus.INTERNAL_SERVER_ERROR, "XML 파싱 중 서버 오류가 발생했습니다."),

  // CRAWLER, LLM
  EXTERNAL_ARTICLE_CRAWL_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "기사 본문 크롤링 처리 중 오류가 발생했습니다."),
  EXTERNAL_LLM_INVALID_INPUT(HttpStatus.INTERNAL_SERVER_ERROR, "LLM 요약 입력값이 유효하지 않습니다."),
  EXTERNAL_LLM_PROVIDER_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "LLM 요약 처리 중 외부 연동 오류가 발생했습니다."),

  // AWS
  AWS_SERVER_CONNECT_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "AWS에 연결 실패했습니다.");

  private final HttpStatus status;
  private final String message;

  ErrorCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;

  }
}
