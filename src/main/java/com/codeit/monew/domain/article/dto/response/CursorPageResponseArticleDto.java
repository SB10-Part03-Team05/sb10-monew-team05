package com.codeit.monew.domain.article.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "커서 기반 페이지 응답")
public record CursorPageResponseArticleDto(

    @Schema(description = "페이지 내용")
    List<ArticleDto> content,

    @Schema(description = "다음 페이지 커서")
    String nextCursor,

    @Schema(description = "다음 보조 커서(마지막 요소의 생성 시간", example = "2026-04-17T16:33:59.000Z")
    Instant nextAfter,

    @Schema(description = "페이지 크기")
    int size,

    @Schema(description = "총 요소 수", example = "100")
    long totalElements,

    @Schema(description = "다음 페이지 여부", example = "true")
    boolean hasNext
) {

}
