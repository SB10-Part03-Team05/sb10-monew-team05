package com.codeit.monew.global.event;

import java.util.UUID;

public record CommentDeletedEvent(
    UUID commentId
) {

}
