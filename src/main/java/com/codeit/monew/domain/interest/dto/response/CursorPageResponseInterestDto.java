package com.codeit.monew.domain.interest.dto.response;

import java.util.List;

public record CursorPageResponseInterestDto(
    List<InterestDto> content,
    String nextCursor,
    int size,
    long totalElements,
    boolean hasNext
) {}
