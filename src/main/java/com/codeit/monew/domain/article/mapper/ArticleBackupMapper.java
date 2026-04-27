package com.codeit.monew.domain.article.mapper;

import com.codeit.monew.domain.article.dto.backup.ArticleBackupDto;
import com.codeit.monew.domain.article.entity.Article;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ArticleBackupMapper {

  @Mapping(target = "interestIds", expression = "java(toInterestIds(article))")
  ArticleBackupDto toDto(Article article);

  // `default` interface 내부에 구현 부분이 있는 메서드를 만들기 위한 문법
  default List<UUID> toInterestIds(Article article) {
    if (article.getArticleInterests() == null) {
      return List.of();
    }

    return article.getArticleInterests().stream()
        .map(articleInterest -> articleInterest.getInterest())
        .filter(interest -> interest != null && interest.getId() != null)
        .map(interest -> interest.getId())
        .toList();
  }
}
