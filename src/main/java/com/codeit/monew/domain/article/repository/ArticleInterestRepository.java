package com.codeit.monew.domain.article.repository;

import com.codeit.monew.domain.article.entity.ArticleInterest;
import com.codeit.monew.domain.article.entity.ArticleInterestId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleInterestRepository extends JpaRepository<ArticleInterest, ArticleInterestId> {

  @Modifying
  @Query(value = """
      INSERT INTO article_interests (article_id, interest_id)
      SELECT :articleId, :interestId
      WHERE EXISTS (SELECT 1 FROM interests WHERE id = :interestId)
            AND NOT EXISTS (
                  SELECT 1 FROM article_interests
                  WHERE article_id = :articleId
                    AND interest_id = :interestId
            )
      """,
      nativeQuery = true)
  int insertArticleInterestIfNotExists(
      @Param("articleId") UUID articleId,
      @Param("interestId") UUID interestId
  );
}
