package com.codeit.monew.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(description = "댓글 등록 정보")
public record CommentRegisterRequest(

    @Schema(description = "기사 ID")
    @NotNull(message = "기사 ID는 필수입니다.")
    UUID articleId,

    @Schema(description = "요청 사용자 ID")
    @NotNull(message = "사용자 ID는 필수입니다.")
    UUID userId,

    @Schema(description = "댓글 내용", maxLength = 500)
    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(min = 1, max = 500, message = "댓글 내용은 1자 이상 500자 이하여야 합니다.")
    String content
) {
}
