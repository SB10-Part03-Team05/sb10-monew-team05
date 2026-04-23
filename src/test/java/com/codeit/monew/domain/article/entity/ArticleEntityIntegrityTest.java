package com.codeit.monew.domain.article.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.global.exception.article.InvalidArticleEntityException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
    // given & when: 유효한 데이터로 기사 생성을 시도
    Article article = validArticle();

    // then: 모든 필드가 의도한 대로 초기화되었는지 검증
    assertEquals(ArticleSource.NAVER, article.getSource());
    assertEquals("https://news.example.com/a", article.getSourceUrl());
    assertEquals("정상 제목", article.getTitle());
    assertEquals(PUBLISH_DATE, article.getPublishDate());
    assertEquals("정상 요약", article.getSummary());
  }

  @Nested
  @DisplayName("필수 값 존재 검증 (Null Integrity)")
  class NullIntegrity {

    @Test
    @DisplayName("source가 null이면 예외")
    void throw_when_source_null() {
      // when & then: source가 null일 때 예외 발생 및 사유 검증
      InvalidArticleEntityException ex = assertThrows(
          InvalidArticleEntityException.class,
          () -> Article.createArticle(null, "url", "제목", PUBLISH_DATE, "요약")
      );
      assertEquals("source", ex.getDetails().get("field"));
      assertEquals("null", ex.getDetails().get("reason"));
    }

    @Test
    @DisplayName("sourceUrl이 null이면 예외")
    void throw_when_source_url_null() {
      // when & then: sourceUrl이 null일 때 예외 발생 및 사유 검증
      InvalidArticleEntityException ex = assertThrows(
          InvalidArticleEntityException.class,
          () -> Article.createArticle(ArticleSource.NAVER, null, "제목", PUBLISH_DATE, "요약")
      );
      assertEquals("sourceUrl", ex.getDetails().get("field"));
      assertEquals("null", ex.getDetails().get("reason"));
    }

    @Test
    @DisplayName("title이 null이면 예외")
    void throw_when_title_null() {
      // when & then: title이 null일 때 예외 발생 및 사유 검증
      InvalidArticleEntityException ex = assertThrows(
          InvalidArticleEntityException.class,
          () -> Article.createArticle(ArticleSource.NAVER, "url", null, PUBLISH_DATE, "요약")
      );
      assertEquals("title", ex.getDetails().get("field"));
      assertEquals("null", ex.getDetails().get("reason"));
    }

    @Test
    @DisplayName("publishDate가 null이면 예외")
    void throw_when_publish_date_null() {
      // when & then: publishDate가 null일 때 예외 발생 및 사유 검증
      InvalidArticleEntityException ex = assertThrows(
          InvalidArticleEntityException.class,
          () -> Article.createArticle(ArticleSource.NAVER, "url", "제목", null, "요약")
      );
      assertEquals("publishDate", ex.getDetails().get("field"));
      assertEquals("null", ex.getDetails().get("reason"));
    }

    @Test
    @DisplayName("summary가 null이면 예외")
    void throw_when_summary_null() {
      // when & then: summary가 null일 때 예외 발생 및 사유 검증
      InvalidArticleEntityException ex = assertThrows(
          InvalidArticleEntityException.class,
          () -> Article.createArticle(ArticleSource.NAVER, "url", "제목", PUBLISH_DATE, null)
      );
      assertEquals("summary", ex.getDetails().get("field"));
      assertEquals("null", ex.getDetails().get("reason"));
    }
  }

  @Nested
  @DisplayName("도메인 규칙 검증 (Domain Integrity)")
  class DomainIntegrity {

    @Test
    @DisplayName("sourceUrl이 blank면 예외")
    void throw_when_source_url_blank() {
      // when & then: sourceUrl이 공백일 때 예외 발생 및 사유 검증
      InvalidArticleEntityException ex = assertThrows(
          InvalidArticleEntityException.class,
          () -> Article.createArticle(ArticleSource.NAVER, "   ", "제목", PUBLISH_DATE, "요약")
      );
      assertEquals("sourceUrl", ex.getDetails().get("field"));
      assertEquals("blank", ex.getDetails().get("reason"));
    }

    @Test
    @DisplayName("title이 blank면 예외")
    void throw_when_title_blank() {
      // when & then: title이 공백일 때 예외 발생 및 사유 검증
      InvalidArticleEntityException ex = assertThrows(
          InvalidArticleEntityException.class,
          () -> Article.createArticle(ArticleSource.NAVER, "url", " ", PUBLISH_DATE, "요약")
      );
      assertEquals("title", ex.getDetails().get("field"));
      assertEquals("blank", ex.getDetails().get("reason"));
    }

    @Test
    @DisplayName("summary가 blank면 예외")
    void throw_when_summary_blank() {
      // when & then: summary가 공백일 때 예외 발생 및 사유 검증
      InvalidArticleEntityException ex = assertThrows(
          InvalidArticleEntityException.class,
          () -> Article.createArticle(ArticleSource.NAVER, "url", "제목", PUBLISH_DATE, " \n ")
      );
      assertEquals("summary", ex.getDetails().get("field"));
      assertEquals("blank", ex.getDetails().get("reason"));
    }
  }

  @Nested
  @DisplayName("데이터 길이 제약 검증 (Constraint Integrity)")
  class ConstraintIntegrity {

    @Test
    @DisplayName("title 길이 200 초과면 예외")
    void throw_when_title_too_long() {
      // given: 200자를 초과하는 제목 생성
      String tooLongTitle = "a".repeat(201);

      // when & then: 생성 시도 시 too_long 사유와 함께 예외 발생 검증
      InvalidArticleEntityException ex = assertThrows(
          InvalidArticleEntityException.class,
          () -> Article.createArticle(ArticleSource.NAVER, "url", tooLongTitle, PUBLISH_DATE, "요약")
      );
      assertEquals("title", ex.getDetails().get("field"));
      assertEquals("too_long", ex.getDetails().get("reason"));
      assertEquals(200, ex.getDetails().get("maxLength"));
    }

    @Test
    @DisplayName("sourceUrl 길이 2048 초과면 예외")
    void throw_when_source_url_too_long() {
      // given: 2048자를 초과하는 URL 생성
      String tooLongUrl = "h".repeat(2049);

      // when & then: 생성 시도 시 too_long 사유와 함께 예외 발생 검증
      InvalidArticleEntityException ex = assertThrows(
          InvalidArticleEntityException.class,
          () -> Article.createArticle(ArticleSource.NAVER, tooLongUrl, "제목", PUBLISH_DATE, "요약")
      );
      assertEquals("sourceUrl", ex.getDetails().get("field"));
      assertEquals("too_long", ex.getDetails().get("reason"));
      assertEquals(2048, ex.getDetails().get("maxLength"));
    }
  }

  @Nested
  @DisplayName("연관관계 및 컬렉션 무결성 (Collection Integrity)")
  class CollectionIntegrity {

    @Test
    @DisplayName("신규 관심사(ID 없음) 추가 시 참조 동일성으로 중복 방지")
    void deduplicate_by_reference_when_no_id() {
      // given: 기사와 ID가 없는 신규 관심사 생성
      Article article = validArticle();
      Interest interest = Interest.create("경제");

      // when: 동일한 객체 참조를 두 번 추가 시도
      article.addInterest(interest);
      article.addInterest(interest);

      // then: 컬렉션에 1건만 존재해야 함
      assertEquals(1, article.getArticleInterests().size());
    }

    @Test
    @DisplayName("저장된 관심사(ID 있음) 추가 시 UUID 기반으로 중복 방지")
    void deduplicate_by_uuid_when_id_exists() {
      // given: 동일한 UUID를 가진 서로 다른 관심사 객체 생성
      Article article = validArticle();
      UUID sameId = UUID.randomUUID();

      Interest interest1 = Interest.create("경제");
      ReflectionTestUtils.setField(interest1, "id", sameId);

      Interest interest2 = Interest.create("경제");
      ReflectionTestUtils.setField(interest2, "id", sameId);

      // when: 서로 다른 객체이나 ID가 같은 두 관심사를 추가 시도
      article.addInterest(interest1);
      article.addInterest(interest2);

      // then: ID 기반 중복 체크로 인해 1건만 존재해야 함
      assertEquals(1, article.getArticleInterests().size());
    }
  }
}