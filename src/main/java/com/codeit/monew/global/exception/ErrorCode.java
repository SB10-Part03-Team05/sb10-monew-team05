package com.codeit.monew.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // User 관련 에러 코드

    // Article 관련 에러 코드

    // Interest 관련 에러 코드

    // Comment 관련 에러 코드
    COMMENT_UPDATE_FORBIDDEN("댓글 수정 권한이 없습니다."),

    // Server 에러 코드
    INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다."),
    INVALID_REQUEST("잘못된 요청입니다.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }
}