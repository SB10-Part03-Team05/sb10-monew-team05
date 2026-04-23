package com.codeit.monew.domain.useractivity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public record UserActivityDto(
    @Schema(description = "사용자 ID")
    String id,
    @Schema(description = "이메일")
    String email,
    @Schema(description = "닉네임")
    String nickname,
    @Schema(description = "가입한 날짜")
    Instant createdAt,
    @Schema(description = "구독 정보")
    List<ActivitySubscriptionDto> subscriptions,
    @Schema(description = "최근 작성한 댓글 (최대 10건)")
    List<ActivityCommentDto> comments,
    @Schema(description = "최근 좋아요를 누른 댓글 (최대 10건)")
    List<ActivityCommentLikeDto> commentLikes,
    @Schema(description = "최근 본 기사 (최대 10건)")
    List<ActivityArticleViewDto> articleViews
) {
  // 내부 데이터 전달용 Record들

  public record ActivitySubscriptionDto(
      @Schema(description = "구독 정보 ID")
      String id,
      @Schema(description = "관심사 ID")
      String interestId,
      @Schema(description = "관심사 이름")
      String interestName,
      @Schema(description = "관련 키워드 목록")
      List<String> interestKeywords,
      @Schema(description = "구독자 수")
      Long interestSubscriberCount,
      @Schema(description = "구독한 날짜")
      Instant createdAt
  ) {}

  public record ActivityCommentDto(
      @Schema(description = "댓글 ID")
      String id,
      @Schema(description = "기사 ID")
      String articleId,
      @Schema(description = "기사 제목")
      String articleTitle,
      @Schema(description = "작성자 ID")
      String userId,
      @Schema(description = "작성자 닉네임")
      String userNickname,
      @Schema(description = "내용")
      String content,
      @Schema(description = "좋아요 수")
      Long likeCount,
      @Schema(description = "작성된 날짜")
      Instant createdAt
  ) {}

  public record ActivityCommentLikeDto(
      @Schema(description = "좋아요 ID")
      String id,
      @Schema(description = "좋아요한 날짜")
      Instant createdAt,
      @Schema(description = "댓글 ID")
      String commentId,
      @Schema(description = "기사 ID")
      String articleId,
      @Schema(description = "기사 제목")
      String articleTitle,
      @Schema(description = "작성자 ID")
      String commentUserId,
      @Schema(description = "작성자 닉네임")
      String commentUserNickname,
      @Schema(description = "내용")
      String commentContent,
      @Schema(description = "좋아요 수")
      Long commentLikeCount,
      @Schema(description = "작성된 날짜")
      Instant commentCreatedAt
  ) {}

  public record ActivityArticleViewDto(
      @Schema(description = "기사 조회 ID")
      String id,
      @Schema(description = "기사를 조회한 사용자 ID")
      String viewedBy,
      @Schema(description = "기사를 본 날짜")
      Instant createdAt,
      @Schema(description = "기사 ID")
      String articleId,
      @Schema(description = "출처")
      String source, // Enum
      @Schema(description = "원본 기사 URL")
      String sourceUrl,
      @Schema(description = "제목")
      String articleTitle,
      @Schema(description = "날짜")
      Instant articlePublishedDate,
      @Schema(description = "요약")
      String articleSummary,
      @Schema(description = "댓글 수")
      Long articleCommentCount,
      @Schema(description = "조회 수")
      Long articleViewCount
  ) {}
}
