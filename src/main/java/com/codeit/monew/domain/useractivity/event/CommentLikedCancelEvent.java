package com.codeit.monew.domain.useractivity.event;

import java.util.UUID;

public record CommentLikedCancelEvent(
    UUID userId,
    UUID commentId
) {

}
