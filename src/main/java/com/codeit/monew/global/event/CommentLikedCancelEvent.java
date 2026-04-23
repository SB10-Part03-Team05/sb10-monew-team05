package com.codeit.monew.global.event;

import java.util.UUID;

public record CommentLikedCancelEvent(
    UUID userId,
    UUID commentId
) {

}
