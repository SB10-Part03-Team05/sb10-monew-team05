package com.codeit.monew.domain.comment.mapper;

import com.codeit.monew.domain.comment.dto.CommentDto;
import com.codeit.monew.domain.comment.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentMapper {
  @Mapping(target = "id", source = "comment.id")
  @Mapping(target = "articleId", source = "comment.article.id")
  @Mapping(target = "userNickname", source = "nickname")
  @Mapping(target = "likedByMe", source = "isLiked")
  CommentDto toDto(Comment comment, String nickname, boolean isLiked);
}
