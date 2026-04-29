package com.codeit.monew.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "커서 기반 페이지 응답")
public record CursorPageResponseCommentDto(

    @Schema(description = "조회된 댓글 목록")
    List<CommentDto> content,

    @Schema(description = "다음 페이지 커서")
    String nextCursor,

    @Schema(description = "다음 보조 커서(마지막 요소의 생성 시간)", example = "2026-04-17T15:04:05.000Z")
    Instant nextAfter,

    @Schema(description = "페이지 크기", example = "10")
    int size,

    @Schema(description = "총 요소 수", example = "100")
    long totalElements,

    @Schema(description = "다음 페이지 여부", example = "true")
    boolean hasNext
) {
}
