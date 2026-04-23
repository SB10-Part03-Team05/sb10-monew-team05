package com.codeit.monew.global.event;

import com.codeit.monew.domain.article.ArticleSource;
import java.time.Instant;
import java.util.UUID;

public record ArticleViewedEvent(
    UUID articleId,
    UUID viewedBy,
    Instant createdAt,
    ArticleSource source,
    String sourceUrl,
    String articleTitle,
    Instant articlePublishedDate,
    String articleSummary,
    Long articleCommentCount,
    Long articleViewCount
) {

}
