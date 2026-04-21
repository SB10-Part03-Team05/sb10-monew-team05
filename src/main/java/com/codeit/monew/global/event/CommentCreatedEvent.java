package com.codeit.monew.global.event;

import java.time.Instant;
import java.util.UUID;

public record CommentCreatedEvent(
    UUID commentId,
    String articleTitle,
    UUID userId,
    String userNickname,
    String content,
    Long likeCount,
    Instant createdAt
) {

}
