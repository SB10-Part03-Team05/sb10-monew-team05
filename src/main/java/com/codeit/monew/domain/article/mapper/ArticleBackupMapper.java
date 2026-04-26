package com.codeit.monew.domain.article.mapper;

import com.codeit.monew.domain.article.dto.backup.ArticleBackupDto;
import com.codeit.monew.domain.article.entity.Article;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArticleBackupMapper {

  ArticleBackupDto toDto(Article article);
}
