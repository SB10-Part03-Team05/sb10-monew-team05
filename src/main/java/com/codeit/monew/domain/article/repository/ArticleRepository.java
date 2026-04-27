package com.codeit.monew.domain.article.repository;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.entity.Article;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, UUID>, ArticleQueryRepository {

  Optional<Article> findByIdAndDeletedAtIsNull(UUID id);

  @Query("SELECT DISTINCT a.source FROM Article AS a WHERE a.deletedAt IS NULL ORDER BY a.source ASC ")
  List<ArticleSource> findDistinctSource();

  // 뉴스 기사 물리 삭제
  // `@SQLRestriction`로 조회 시 deletedAt이 null인 데이터만 가져오도록 필터링하기 때문에
  // `void` 가 아닌 `int` 로 값을 반환해 id가 존재하는 기사인지 검증도 같이함
  @Modifying // 조회 쿼리가 아님을 JPA에게 명시
  @Query(value = "DELETE FROM articles AS a WHERE a.id = :articleId", nativeQuery = true)
  int hardDelete(@Param("articleId") UUID articleId);

  // 논리 삭제된 기사 포함
  @Query(value = "SELECT EXISTS (SELECT 1 FROM articles WHERE source_url = :sourceUrl)", nativeQuery = true)
  boolean existsBySourceUrl(String sourceUrl);

  @Query(
      value = "SELECT source_url FROM articles WHERE source_url IN (:chunk)",
      nativeQuery = true
  )
  Collection<String> findAllExistingUrlsIn(List<String> chunk);

  @Query(value = """
      SELECT DISTINCT a 
      FROM Article AS a 
      LEFT JOIN FETCH a.articleInterests ai 
      LEFT JOIN FETCH ai.interest 
      WHERE a.publishDate >= :from
            AND a.publishDate < :to
            AND a.deletedAt IS NULL
      """)
  List<Article> findAllWithInterests(
      @Param("from") Instant from,
      @Param("to") Instant to
  );
}
