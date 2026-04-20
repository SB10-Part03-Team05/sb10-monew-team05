package com.codeit.monew.domain.article.dto;

import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import io.swagger.v3.oas.annotations.media.Schema;

public record ArticleScrapeResponse(
    @Schema(description = "수집한 뉴스 소스")
    NewsSourceUrl source,

    @Schema(description = "NAVER 전용 검색어")
    String query,

    @Schema(description = "이번 호출에서 저장된 기사 수")
    int savedCount
) {
}

