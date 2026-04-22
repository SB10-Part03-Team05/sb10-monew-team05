package com.codeit.monew.global.event;

import java.time.Instant;
import java.util.UUID;

public record CommentLikedEvent(
    UUID commentLikeId,
    Instant createdAt,
    UUID commentId,
    UUID articleId,
    UUID commentUserId,
    String commentUserNickname,
    Long commentLikeCount,
    Instant commentCreatedAt
) {

}
