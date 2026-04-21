package com.codeit.monew.domain.useractivity.mapper;

import com.codeit.monew.domain.useractivity.dto.UserActivityDto;
import com.codeit.monew.domain.useractivity.entity.UserActivity;
import com.codeit.monew.global.event.InterestSubscribedEvent;
import com.codeit.monew.global.event.UserRegisteredEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserActivityMapper {
  UserActivityDto toDto(UserActivity userActivity);

  UserActivityDto.ActivitySubscriptionDto toSubscriptionDto(UserActivity.SubscriptionInfo info);

  UserActivityDto.ActivityCommentDto toCommentDto(UserActivity.CommentInfo info);

  UserActivityDto.ActivityCommentLikeDto toCommentLikeDto(UserActivity.CommentLikeInfo info);

  UserActivityDto.ActivityArticleViewDto toArticleViewDto(UserActivity.ArticleViewInfo info);

  @Mapping(target = "id", source = "userId")
  UserActivity toUserActivity(UserRegisteredEvent event);

  @Mapping(target = "id", source = "subscriptionId")
  UserActivity.SubscriptionInfo toSubscriptionInfo(InterestSubscribedEvent event);
}
