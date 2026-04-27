package com.codeit.monew.domain.useractivity.event;

import java.util.UUID;

public record CommentUpdatedEvent(
    UUID userId,
    UUID commentId,
    String newContent
) {

}
