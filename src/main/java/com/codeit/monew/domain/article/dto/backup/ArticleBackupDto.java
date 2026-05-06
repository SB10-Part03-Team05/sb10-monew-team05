package com.codeit.monew.domain.article.dto.backup;

import com.codeit.monew.domain.article.ArticleSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArticleBackupDto(

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

    @Schema(description = "생성 시간")
    Instant createdAt,

    @Schema(description = "수정 시간")
    Instant updatedAt,

    @Schema(description = "삭제 시간")
    Instant deletedAt,

    @Schema(description = "연결된 관심사 ID 목록")
    List<UUID> interestIds
) {

}
