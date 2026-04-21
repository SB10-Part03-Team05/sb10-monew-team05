package com.codeit.monew.domain.article.mapper;

import com.codeit.monew.domain.article.dto.response.ArticleViewDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleViewHistory;
import com.codeit.monew.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ArticleViewMapper {

  @Mapping(target = "id", source = "articleViewHistory.id")
  @Mapping(target = "viewedBy", source = "user.id")
  @Mapping(target = "createdAt", source = "articleViewHistory.createdAt")
  @Mapping(target = "articleId", source = "article.id")
  @Mapping(target = "source", source = "article.source")
  @Mapping(target = "sourceUrl", source = "article.sourceUrl")
  @Mapping(target = "articleTitle", source = "article.title")
  @Mapping(target = "articlePublishedDate", source = "article.publishDate")
  @Mapping(target = "articleSummary", source = "article.summary")
  @Mapping(target = "articleCommentCount", source = "commentCount")
  @Mapping(target = "articleViewCount", source = "viewCount")
  ArticleViewDto toDto(
      ArticleViewHistory articleViewHistory,
      Article article,
      User user,
      long commentCount,
      long viewCount
  );
}
