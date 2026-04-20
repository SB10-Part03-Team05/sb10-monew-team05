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

  @Query("SELECT DISTINCT a.source FROM Article AS a WHERE a.deletedAt IS NULL ORDER BY a.source ASC ")
  List<ArticleSource> findDistinctSource();

  // 논리 삭제된 기사 포함
  @Query(value = "SELECT EXISTS (SELECT 1 FROM articles WHERE source_url = :sourceUrl)", nativeQuery = true)
  boolean existsBySourceUrl(String sourceUrl);
}
