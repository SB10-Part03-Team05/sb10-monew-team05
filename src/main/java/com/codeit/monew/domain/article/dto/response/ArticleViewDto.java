package com.codeit.monew.domain.article.dto.response;

import com.codeit.monew.domain.article.ArticleSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record ArticleViewDto(

    @Schema(description = "뉴스 기사 조회 ID")
    UUID id,

    @Schema(description = "뉴스 기사를 조회한 사용자 ID")
    UUID viewedBy,

    @Schema(description = "뉴스 기사를 본 날짜")
    Instant createdAt,

    @Schema(description = "뉴스 기사 ID")
    UUID articleId,

    @Schema(description = "출처")
    ArticleSource source,

    @Schema(description = "원본 기사 URL")
    String sourceUrl,

    @Schema(description = "제목")
    String articleTitle,

    @Schema(description = "날짜")
    Instant articlePublishedDate,

    @Schema(description = "요약")
    String articleSummary,

    @Schema(description = "댓글 수")
    long articleCommentCount,

    @Schema(description = "조회 수")
    long articleViewCount
) {

}
