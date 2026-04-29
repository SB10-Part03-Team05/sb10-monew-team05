package com.codeit.monew.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record CommentLikeDto(

    @Schema(description = "좋아요 ID")
    UUID id,

    @Schema(description = "좋아요한 사용자 ID")
    UUID likedBy,

    @Schema(description = "좋아요한 날짜")
    Instant createdAt,

    @Schema(description = "댓글 ID")
    UUID commentId,

    @Schema(description = "기사 ID")
    UUID articleId,

    @Schema(description = "댓글 작성자 ID")
    UUID commentUserId,

    @Schema(description = "댓글 작성자 닉네임")
    String commentUserNickname,

    @Schema(description = "댓글 내용")
    String commentContent,

    @Schema(description = "댓글 좋아요 수")
    Long commentLikeCount,

    @Schema(description = "작성된 날짜")
    Instant commentCreatedAt
) {
}