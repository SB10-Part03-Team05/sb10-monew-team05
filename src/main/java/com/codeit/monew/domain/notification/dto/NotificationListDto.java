package com.codeit.monew.domain.notification.dto;

import java.time.Instant;
import java.util.List;

public record NotificationListDto(
    List<NotificationDto> content,
    String nextCursor,
    Instant nextAfter,
    int size,
    long totalElements,
    boolean hasNext
) {

}
