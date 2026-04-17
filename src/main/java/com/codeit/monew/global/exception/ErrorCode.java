package com.codeit.monew.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
  // common
  INVALID_PARAMETER_INPUT("입력값이 유효하지 않습니다."),

  // User 관련 에러 코드
  USER_NOT_FOUND("유저를 찾을 수 없습니다."),
  DUPLICATE_EMAIL("이미 사용중인 이메일입니다."),

  // Article 관련 에러 코드
  ARTICLE_NOT_FOUND("뉴스 기사 정보를 찾을 수 없습니다."),

  // Interest 관련 에러 코드
  INTEREST_NOT_FOUND("관심사 정보를 찾을 수 없습니다."),

  // Comment 관련 에러 코드
  COMMENT_UPDATE_FORBIDDEN("댓글 수정 권한이 없습니다."),
  COMMENT_CONTENT_BLANK("댓글 내용은 비어있을 수 없습니다."),
  COMMENT_CONTENT_TOO_LONG("댓글 내용은 500자를 초과할 수 없습니다."),

  // Server 에러 코드
  INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다."),
  INVALID_REQUEST("잘못된 요청입니다.");

  private final String message;

  ErrorCode(String message) {
    this.message = message;
  }
}