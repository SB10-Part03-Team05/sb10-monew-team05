package com.codeit.monew.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "조회할 알림 목록 정보")
public record NotificationListDto(

    @Schema(description = "조회된 알림 목록")
    List<NotificationDto> content,

    @Schema(description = "커서 값")
    String nextCursor,

    @Schema(description = "보조 커서(createdAt) 값")
    Instant nextAfter,

    @Schema(description = "커서 페이지 크기")
    int size,

    @Schema(description = "전체 데이터 개수")
    long totalElements,

    @Schema(description = "다음 페이지 존재 여부")
    boolean hasNext
) {

}
