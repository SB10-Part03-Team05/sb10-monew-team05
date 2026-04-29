package com.codeit.monew.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record CommentDto(

    @Schema(description = "댓글 ID")
    UUID id,

    @Schema(description = "기사 ID")
    UUID articleId,

    @Schema(description = "댓글 작성자 ID")
    UUID userId,

    @Schema(description = "댓글 작성자 닉네임")
    String userNickname,

    @Schema(description = "댓글 내용")
    String content,

    @Schema(description = "댓글 좋아요 수")
    Long likeCount,

    @Schema(description = "요청자의 좋아요 여부")
    boolean likedByMe,

    @Schema(description = "작성된 날짜")
    Instant createdAt
) {
}