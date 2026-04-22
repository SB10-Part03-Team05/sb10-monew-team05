package com.codeit.monew.domain.comment.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.UUID;

// 댓글 목록 조회 요청 dto
public record CommentCursorRequest(
    @Parameter(description = "기사 ID", required = true)
    @NotNull(message = "기사 ID는 필수입니다.")
    UUID articleId,

    @Parameter(description = "정렬 기준 (createdAt: 최신순, likeCount: 좋아요순)", example = "createdAt")
    @Pattern(regexp = "^(createdAt|likeCount)$", message = "정렬 기준은 createdAt 또는 likeCount여야 합니다.")
    String orderBy,

    @Parameter(description = "정렬 방향 (ASC: 오름차순, DESC: 내림차순)", example = "DESC")
    @Pattern(regexp = "^(ASC|DESC)$", message = "정렬 방향은 ASC 또는 DESC여야 합니다.")
    String direction,

    @Parameter(description = "커서 값 (주 정렬 기준의 마지막 값)", example = "10")
    String cursor,

    @Parameter(description = "보조 커서 값 (createdAt, 동점자 처리용)", example = "2026-04-21T05:56:10.574Z")
    Instant after,

    @Parameter(description = "한 페이지에 불러올 개수 (최대 100)", example = "10")
    @Min(value = 1, message = "최소 1개 이상 조회해야 합니다.")
    @Max(value = 100, message = "최대 100개까지 조회 가능합니다.")
    Integer limit
) {
  // 컴팩트 생성자를 통한 기본값 설정
  public CommentCursorRequest {
    if (orderBy == null) orderBy = "createdAt";
    if (direction == null) direction = "DESC";
    if (limit == null) limit = 10;
  }
}
