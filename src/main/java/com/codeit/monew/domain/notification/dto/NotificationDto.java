package com.codeit.monew.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record NotificationDto(

    @Schema(description = "알림 ID")
    UUID id,

    @Schema(description = "알림 생성 일시")
    Instant createdAt,

    @Schema(description = "알림 수정 일시")
    Instant updatedAt,

    @Schema(description = "알림 확인 여부")
    boolean confirmed,

    @Schema(description = "알림을 받을 사용자 ID")
    UUID userId,

    @Schema(description = "알림 내용")
    String content,

    @Schema(description = "관련 리소스 타입 (INTEREST 또는 COMMENT)")
    String resourceType,

    @Schema(description = "관련 리소스 ID (관심사 ID 또는 댓글 ID)")
    UUID resourceId
) {

}
