package com.codeit.monew.global.event;

import java.time.Instant;
import java.util.UUID;

public record CommentLikedEvent(
    UUID userId,
    UUID commentLikeId,
    Instant createdAt,
    UUID commentId,
    UUID articleId,
    String articleTitle,
    UUID commentUserId,
    String commentUserNickname,
    String commentContent,
    Long commentLikeCount,
    Instant commentCreatedAt
) {

}
