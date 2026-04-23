package com.codeit.monew.global.event;

import java.util.UUID;

public record CommentUpdatedEvent(
    UUID userId,
    UUID commentId,
    String newContent
) {

}
