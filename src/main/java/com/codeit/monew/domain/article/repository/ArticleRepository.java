package com.codeit.monew.domain.article.repository;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.entity.Article;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

  Optional<Article> findByIdAndDeletedAtIsNull(UUID id);

  @Query("SELECT DISTINCT source FROM Article ORDER BY source ASC ")
  List<ArticleSource> findDistinctSource();
}
