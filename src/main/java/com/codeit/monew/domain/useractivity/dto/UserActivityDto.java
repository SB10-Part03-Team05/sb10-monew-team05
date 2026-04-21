package com.codeit.monew.domain.useractivity.dto;

import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.interest.dto.response.SubscriptionDto;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public record UserActivityDto(
    String id,
    String email,
    String nickname,
    Instant createdAt,
    List<ActivitySubscriptionDto> subscriptions,
    List<ActivityCommentDto> comments,
    List<ActivityCommentLikeDto> commentLikes,
    List<ActivityArticleViewDto> articleViews
) {
  // 내부 데이터 전달용 Record들

  public record ActivitySubscriptionDto(
      String id,
      String interestId,
      String interestName,
      String interestKeywords,
      Long interestSubscriberCount,
      Instant createdAt
  ) {}

  public record ActivityCommentDto(
      String id,
      String articleId,
      String articleTitle,
      String content,
      Long likeCount,
      Instant createdAt
  ) {}

  public record ActivityCommentLikeDto(
      String id,
      Instant createdAt,
      String articleId,
      String articleTitle,
      String commentUserId,
      String commentUserNickname,
      String commentContent,
      Long commentLikeCount,
      Instant commentCreatedAt
  ) {}

  public record ActivityArticleViewDto(
      String id,
      String viewedBy,
      Instant createdAt,
      String articleId,
      String source, // Enum
      String sourceUrl,
      String articleTitle,
      Instant articlePublishedDate,
      String articleSummary,
      Long articleCommentCount,
      Long articleViewCount
  ) {}
}
