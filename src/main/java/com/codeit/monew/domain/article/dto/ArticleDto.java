package com.codeit.monew.domain.article.dto;

import com.codeit.monew.domain.article.ArticleSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record ArticleDto(
    @Schema(description = "기사 ID")
    UUID id,

    @Schema(description = "출처")
    ArticleSource source,

    @Schema(description = "원본 기사 URL")
    String sourceUrl,

    @Schema(description = "제목")
    String title,

    @Schema(description = "날짜")
    Instant publishDate,

    @Schema(description = "요약")
    String summary,

    @Schema(description = "댓글 수")
    long commentCount,

    @Schema(description = "조회 수")
    long viewCount,

    @Schema(description = "요청자의 조회 여부")
    boolean viewedByMe
) {

}
