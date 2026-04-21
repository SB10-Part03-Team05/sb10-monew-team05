package com.codeit.monew.domain.useractivity.mapper;

import com.codeit.monew.domain.useractivity.dto.UserActivityDto;
import com.codeit.monew.domain.useractivity.entity.UserActivity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserActivityMapper {
  UserActivityDto toDto(UserActivity userActivity);

  UserActivityDto.ActivitySubscriptionDto toSubscriptionDto(UserActivity.SubscriptionInfo info);

  UserActivityDto.ActivityCommentDto toCommentDto(UserActivity.CommentInfo info);

  UserActivityDto.ActivityCommentLikeDto toCommentLikeDto(UserActivity.CommentLikeInfo info);

  UserActivityDto.ActivityArticleViewDto toArticleViewDto(UserActivity.ArticleViewInfo info);
}
