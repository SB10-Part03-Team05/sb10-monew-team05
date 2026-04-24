package com.codeit.monew.domain.useractivity.mapper;

import com.codeit.monew.domain.useractivity.dto.UserActivityDto;
import com.codeit.monew.domain.useractivity.entity.UserActivity;
import com.codeit.monew.domain.useractivity.event.ArticleViewedEvent;
import com.codeit.monew.domain.useractivity.event.CommentCreatedEvent;
import com.codeit.monew.domain.useractivity.event.CommentLikedEvent;
import com.codeit.monew.domain.useractivity.event.InterestSubscribedEvent;
import com.codeit.monew.domain.useractivity.event.UserRegisteredEvent;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
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

  @Mapping(target = "id", source = "commentId")
  UserActivity.CommentInfo toCommentInfo(CommentCreatedEvent event);

  @Mapping(target = "id", source = "commentLikeId")
  UserActivity.CommentLikeInfo toCommentLikeInfo(CommentLikedEvent event);

  @Mapping(target = "id", source = "articleId")
  UserActivity.ArticleViewInfo toArticleViewInfo(ArticleViewedEvent event);
}
