package com.codeit.monew.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "커서 기반 페이지 응답")
public record CursorPageResponseCommentDto(

    @Schema(description = "조회된 댓글 목록")
    List<CommentDto> content,

    @Schema(description = "커서 값")
    String nextCursor,

    @Schema(description = "보조 커서(createdAt) 값")
    Instant nextAfter,

    @Schema(description = "커서 페이지 크기")
    int size,

    @Schema(description = "전체 데이터 개수")
    long totalElements,

    @Schema(description = "다음 페이지 존재 여부")
    boolean hasNext
) {
}
