package com.codeit.monew.domain.interest.dto.response;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record CursorPageResponseInterestDto(
    List<InterestDto> content,
    String nextCursor,
    Instant nextAfter,
    int size,
    long totalElements,
    boolean hasNext
) {}
