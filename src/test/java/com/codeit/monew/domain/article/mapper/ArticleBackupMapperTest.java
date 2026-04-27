package com.codeit.monew.domain.article.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.dto.backup.ArticleBackupDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.interest.entity.Interest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

public class ArticleBackupMapperTest {

  // MapStruct가 생성한 mapper 구현체를 테스트에 직접 가져옴
  private final ArticleBackupMapper articleBackupMapper = Mappers.getMapper(
      ArticleBackupMapper.class);

  private Article createArticle(UUID articleId, ArticleSource source, String sourceUrl,
      String title, Instant publishDate, String summary) {
    Article article = Article.createArticle(source, sourceUrl, title, publishDate, summary);

    if (articleId == null) {
      ReflectionTestUtils.setField(article, "id", UUID.randomUUID());
    } else {
      ReflectionTestUtils.setField(article, "id", articleId);
    }

    return article;
  }

  private Interest createInterest(UUID interestId, String name) {
    Interest interest = Interest.create(name);

    if (interestId == null) {
      ReflectionTestUtils.setField(interest, "id", UUID.randomUUID());
    } else {
      ReflectionTestUtils.setField(interest, "id", interestId);
    }

    return interest;
  }

  @Test
  @DisplayName("Mapper로 ArticleBackupDto를 만들 수 있다.")
  void return_interest_id_list() {
    // given
    Article article = createArticle(null, ArticleSource.NAVER, "https://naver.com", "title",
        Instant.now(), "summary");
    Interest interest = createInterest(null, "삼성");
    article.addInterest(interest);

    // when
    ArticleBackupDto result = articleBackupMapper.toDto(article);

    // then
    assertEquals(interest.getId(), result.interestIds().get(0));
  }

  @Test
  @DisplayName("뉴스 기사와 매핑된 관심사가 없을 경우(null`), 빈 배열을 반환한다.")
  void returns_empty_list_when_articleInterests_is_null() {
    // given
    Article article = createArticle(null, ArticleSource.NAVER, "https://naver.com", "title",
        Instant.now(), "summary");
    ReflectionTestUtils.setField(article, "articleInterests", null);

    // when
    List<UUID> result = articleBackupMapper.toInterestIds(article);

    // then
    assertEquals(0, result.size());
  }
}
