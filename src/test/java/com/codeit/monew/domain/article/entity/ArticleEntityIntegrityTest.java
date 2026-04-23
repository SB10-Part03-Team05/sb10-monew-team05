package com.codeit.monew.domain.article.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.global.exception.article.InvalidArticleEntityException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ArticleEntityIntegrityTest {

  private static final Instant PUBLISH_DATE = Instant.parse("2026-04-23T00:00:00Z");

  private Article validArticle() {
    return Article.createArticle(
        ArticleSource.NAVER,
        "https://news.example.com/a",
        "정상 제목",
        PUBLISH_DATE,
        "정상 요약"
    );
  }

  @Test
  @DisplayName("유효한 값으로 Article 생성 성공")
  void create_success() {
    // given & when
    Article article = validArticle();

    // then
    assertEquals(ArticleSource.NAVER, article.getSource());
    assertEquals("https://news.example.com/a", article.getSourceUrl());
    assertEquals("정상 제목", article.getTitle());
    assertEquals(PUBLISH_DATE, article.getPublishDate());
    assertEquals("정상 요약", article.getSummary());
  }

  @Test
  @DisplayName("source가 null이면 예외")
  void create_fail_when_source_null() {
    // when & then
    InvalidArticleEntityException ex = assertThrows(
        InvalidArticleEntityException.class,
        () -> Article.createArticle(null, "https://a.com", "제목", PUBLISH_DATE, "요약")
    );

    assertEquals("source", ex.getDetails().get("field"));
    assertEquals("null", ex.getDetails().get("reason"));
  }

  @Test
  @DisplayName("sourceUrl이 blank면 예외")
  void create_fail_when_source_url_blank() {
    // when & then
    InvalidArticleEntityException ex = assertThrows(
        InvalidArticleEntityException.class,
        () -> Article.createArticle(ArticleSource.NAVER, "   ", "제목", PUBLISH_DATE, "요약")
    );

    assertEquals("sourceUrl", ex.getDetails().get("field"));
    assertEquals("blank", ex.getDetails().get("reason"));
  }

  @Test
  @DisplayName("title이 blank면 예외")
  void create_fail_when_title_blank() {
    // when & then
    InvalidArticleEntityException ex = assertThrows(
        InvalidArticleEntityException.class,
        () -> Article.createArticle(ArticleSource.NAVER, "https://a.com", "   ", PUBLISH_DATE, "요약")
    );

    assertEquals("title", ex.getDetails().get("field"));
    assertEquals("blank", ex.getDetails().get("reason"));
  }

  @Test
  @DisplayName("publishDate가 null이면 예외")
  void create_fail_when_publish_date_null() {
    // when & then
    InvalidArticleEntityException ex = assertThrows(
        InvalidArticleEntityException.class,
        () -> Article.createArticle(ArticleSource.NAVER, "url", "제목", null, "요약")
    );

    assertEquals("publishDate", ex.getDetails().get("field"));
    assertEquals("null", ex.getDetails().get("reason"));
  }

  @Test
  @DisplayName("summary가 blank면 예외")
  void create_fail_when_summary_blank() {
    // when & then
    InvalidArticleEntityException ex = assertThrows(
        InvalidArticleEntityException.class,
        () -> Article.createArticle(ArticleSource.NAVER, "url", "제목", PUBLISH_DATE, "")
    );

    assertEquals("summary", ex.getDetails().get("field"));
    assertEquals("blank", ex.getDetails().get("reason"));
  }

  @Test
  @DisplayName("title 길이 200 초과면 예외")
  void create_fail_when_title_too_long() {
    // given
    String tooLongTitle = "a".repeat(201);

    // when & then
    InvalidArticleEntityException ex = assertThrows(
        InvalidArticleEntityException.class,
        () -> Article.createArticle(ArticleSource.NAVER, "https://a.com", tooLongTitle,
            PUBLISH_DATE, "요약")
    );

    assertEquals("title", ex.getDetails().get("field"));
    assertEquals("too_long", ex.getDetails().get("reason"));
    assertEquals(200, ex.getDetails().get("maxLength"));
  }

  @Test
  @DisplayName("sourceUrl 길이 2048 초과면 예외")
  void create_fail_when_source_url_too_long() {
    // given
    String tooLongUrl = "h".repeat(2049);

    // when & then
    InvalidArticleEntityException ex = assertThrows(
        InvalidArticleEntityException.class,
        () -> Article.createArticle(ArticleSource.NAVER, tooLongUrl, "제목", PUBLISH_DATE, "요약")
    );

    assertEquals("sourceUrl", ex.getDetails().get("field"));
    assertEquals("too_long", ex.getDetails().get("reason"));
    assertEquals(2048, ex.getDetails().get("maxLength"));
  }

  @Test
  @DisplayName("신규 관심사(ID 없음) 추가 시 참조 동일성으로 중복 방지")
  void add_interest_deduplicate_by_reference() {
    // given
    Article article = validArticle();
    Interest interest = Interest.create("경제");

    // when
    article.addInterest(interest);
    article.addInterest(interest); // 같은 객체 참조 추가 시도

    // then
    assertEquals(1, article.getArticleInterests().size());
  }

  @Test
  @DisplayName("저장된 관심사(ID 있음) 추가 시 UUID 기반으로 중복 방지")
  void add_interest_deduplicate_by_uuid() {
    // given: 동일한 UUID를 가진 두 개의 관심사 객체 생성
    Article article = validArticle();
    UUID sameId = UUID.randomUUID();

    Interest interest1 = Interest.create("경제");
    ReflectionTestUtils.setField(interest1, "id", sameId);

    Interest interest2 = Interest.create("경제");
    ReflectionTestUtils.setField(interest2, "id", sameId);

    // when: 서로 다른 객체지만 ID가 같은 두 관심사 추가 시도
    article.addInterest(interest1);
    article.addInterest(interest2);

    // then: ID 기반 비교를 통해 중복으로 판단, 1건만 유지되어야 함
    assertEquals(1, article.getArticleInterests().size());
  }
}