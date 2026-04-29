package com.codeit.monew.domain.interest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "커서 기반 페이지 응답")
public record CursorPageResponseInterestDto(
    @Schema(description = "페이지 내용")
    List<InterestDto> content,

    @Schema(description = "다음 페이지 커서")
    String nextCursor,

    @Schema(description = "페이지 크기", example = "10")
    int size,

    @Schema(description = "총 요소 수", maxLength = 100)
    long totalElements,

    @Schema(description = "다음 페이지 여부", example = "true")
    boolean hasNext
) {}
