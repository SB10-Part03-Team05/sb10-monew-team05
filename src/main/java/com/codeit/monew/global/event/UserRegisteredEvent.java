package com.codeit.monew.global.event;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
    UUID userId,
    String email,
    String nickname,
    Instant createdAt
) {
}
