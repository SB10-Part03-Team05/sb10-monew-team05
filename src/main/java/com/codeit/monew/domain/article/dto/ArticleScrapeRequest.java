package com.codeit.monew.domain.article.dto;

import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import io.swagger.v3.oas.annotations.media.Schema;

public record ArticleScrapeRequest(
    @Schema(description = "수집할 뉴스 소스", example = "HANKYUNG")
    NewsSourceUrl source,

    @Schema(description = "NAVER 전용 검색어 (NAVER가 아닐 경우 null 가능)", example = "반도체")
    String query
) {
}

