package com.codeit.monew.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
  // common
  INVALID_PARAMETER_INPUT("입력값이 유효하지 않습니다."),

  // User 관련 에러 코드
  USER_NOT_FOUND("사용자를 찾을 수 없습니다."),
  DUPLICATE_EMAIL("이미 사용중인 이메일입니다."),
  USER_ACCESS_DENIED("사용자 수정, 삭제 권한이 없습니다."),
  PASSWORD_MISMATCH("비밀번호가 일치하지 않습니다."),

  // Article 관련 에러 코드
  ARTICLE_NOT_FOUND("뉴스 기사 정보를 찾을 수 없습니다."),
  INVALID_ARTICLE_ENTITY("뉴스 기사 정보가 유효하지 않습니다."),
  ARTICLE_SCRAPE_FAILED("뉴스 기사 수집 처리 중 오류가 발생했습니다."),

  // Interest 관련 에러 코드
  INTEREST_NOT_FOUND("관심사를 찾을 수 없습니다."),
  DUPLICATE_INTEREST("유사한 관심사가 이미 존재합니다."),
  ALREADY_SUBSCRIBED("이미 구독 중인 관심사입니다."),
  SUBSCRIPTION_NOT_FOUND("구독 정보를 찾을 수 없습니다."),

  // Comment 관련 에러 코드
  COMMENT_UPDATE_FORBIDDEN("댓글 수정 권한이 없습니다."),
  COMMENT_CONTENT_BLANK("댓글 내용은 비어있을 수 없습니다."),
  COMMENT_CONTENT_TOO_LONG("댓글 내용은 500자를 초과할 수 없습니다."),
  COMMENT_NOT_FOUND("댓글 정보를 찾을 수 없습니다."),
  COMMENT_LIKE_ALREADY_EXISTS("이미 좋아요를 누른 댓글입니다."),
  COMMENT_LIKE_NOT_FOUND("좋아요 정보가 없습니다."),

  // Notification 관련 에러 코드
  NOTIFICATION_NOT_FOUND("알림 정보를 찾을 수 없습니다."),
  NOTIFICATION_ACCESS_DENIED("해당 알림에 접근할 권한이 없습니다."),
  NOTIFICATION_RECEIVER_MISMATCH("댓글 작성자와 알림 수신자가 일치하지 않습니다."),

  // Server 에러 코드
  INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다."),
  INVALID_REQUEST("잘못된 요청입니다."),

  // External 관련 에러 코드
  EXTERNAL_CLIENT_ERROR("외부 연동 요청 오류가 발생했습니다."),
  EXTERNAL_RATE_LIMITED("외부 연동 요청 제한이 발생했습니다."),
  EXTERNAL_SERVER_ERROR("외부 연동 서버 오류가 발생했습니다."),
  EXTERNAL_EMPTY_RESPONSE("외부 연동 응답 본문이 비어 있습니다."),
  EXTERNAL_NETWORK_ERROR("외부 연동 네트워크 오류가 발생했습니다."),
  EXTERNAL_INVALID_XML("외부 연동 XML 파싱에 실패했습니다.");

  private final String message;

  ErrorCode(String message) {
    this.message = message;
  }
}
