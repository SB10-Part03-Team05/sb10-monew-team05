package com.codeit.monew.domain.notification.mapper;

import com.codeit.monew.domain.notification.dto.NotificationDto;
import com.codeit.monew.domain.notification.entity.CommentNotification;
import com.codeit.monew.domain.notification.entity.InterestNotification;
import com.codeit.monew.domain.notification.entity.Notification;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

  @Mapping(target = "userId", source = "user.id")
  @Mapping(target = "resourceType", source = "notification", qualifiedByName = "mapResourceType")
  @Mapping(target = "resourceId", source = "notification", qualifiedByName = "mapResourceId")
  NotificationDto toDto(Notification notification);

  @Named("mapResourceType")
  default String mapResourceType(Notification notification) {
    if (notification instanceof InterestNotification) return "interest";
    if (notification instanceof CommentNotification) return "comment";
    return null;
  }

  @Named("mapResourceId")
  default UUID mapResourceId(Notification notification) {
    if (notification instanceof InterestNotification interest) return interest.getInterest().getId();
    if (notification instanceof CommentNotification comment) return comment.getComment().getId();
    return null;
  }
}
