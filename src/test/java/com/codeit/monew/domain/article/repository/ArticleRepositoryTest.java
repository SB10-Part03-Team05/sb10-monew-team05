package com.codeit.monew.domain.article.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.global.config.JpaAuditingConfig;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class ArticleRepositoryTest {

  @Autowired
  private ArticleRepository articleRepository;

  @Autowired
  private TestEntityManager testEntityManager;

  @BeforeEach
  void setUp() {
    articleRepository.deleteAll();
  }

  private Article createArticle(ArticleSource source, String sourceUrl,
      String title, Instant publishDate, String summary) {
    Article article = Article.createArticle(source, sourceUrl, title, publishDate, summary);

    return articleRepository.save(article);
  }

  @Test
  @DisplayName("저장된 뉴스 기사의 출처 목록을 중복 없이 조회할 수 있다.")
  void findDistinctSource() {
    // given(준비)
    Article article1 = createArticle(ArticleSource.NAVER, "https://naver1.com", "title1",
        Instant.now(), "summary1");
    Article article2 = createArticle(ArticleSource.NAVER, "https://naver2.com", "title2",
        Instant.now(), "summary2");
    Article article3 = createArticle(ArticleSource.CHOSUN, "https://chosun1.com", "title3",
        Instant.now(), "summary3");

    // 영속성 해제
    testEntityManager.flush();
    testEntityManager.clear();

    // when(실행)
    List<ArticleSource> result = articleRepository.findDistinctSource();

    // then(검증)
    assertThat(result).containsExactly(ArticleSource.CHOSUN, ArticleSource.NAVER);
  }
}