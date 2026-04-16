package com.codeit.monew.domain.article.repository;

import com.codeit.monew.domain.article.entity.Article;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

  Optional<Article> findByIdAndDeletedAtIsNull(UUID id);
}
