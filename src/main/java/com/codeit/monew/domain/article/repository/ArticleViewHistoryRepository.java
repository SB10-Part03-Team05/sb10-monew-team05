package com.codeit.monew.domain.article.repository;

import com.codeit.monew.domain.article.entity.ArticleViewHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleViewHistoryRepository extends JpaRepository<ArticleViewHistory, UUID> {

  long countByArticleId(UUID articleId);

  boolean existsByArticleIdAndUserId(UUID articleId, UUID userId);
}
