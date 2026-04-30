package com.codeit.monew.domain.article.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleInterest;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.interest.entity.Interest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleScrapePersistenceServiceTest {

  @Mock
  private ArticleRepository articleRepository;

  @Mock
  private EntityManager entityManager;

  @InjectMocks
  private ArticleScrapePersistenceService articleScrapePersistenceService;

  private Article article(String url, String title) {
    return Article.createArticle(
        ArticleSource.NAVER,
        url,
        title,
        Instant.parse("2026-04-01T00:00:00Z"),
        "summary"
    );
  }

  private Interest interestWithId(String name, UUID id) {
    Interest interest = Interest.create(name);
    ReflectionTestUtils.setField(interest, "id", id);
    return interest;
  }

  private Interest interestWithoutId(String name) {
    return Interest.create(name);
  }

  @Nested
  @DisplayName("saveAll")
  class SaveAll {

    @Test
    @DisplayName("유효한 interest id만 managed reference로 매핑하고 saveAll 호출")
    void map_valid_interest_ids_and_save_all() {
      // given
      Article a1 = article("https://a.com/1", "a1");
      Article a2 = article("https://a.com/2", "a2");

      UUID id1 = UUID.randomUUID();
      UUID id2 = UUID.randomUUID();
      UUID id3 = UUID.randomUUID();

      Interest raw1 = interestWithId("i1", id1);
      Interest raw2 = interestWithId("i2", id2);
      Interest raw3 = interestWithId("i3", id3);

      Interest managed1 = interestWithId("m1", id1);
      Interest managed2 = interestWithId("m2", id2);
      Interest managed3 = interestWithId("m3", id3);

      org.mockito.Mockito.when(entityManager.getReference(Interest.class, id1))
          .thenReturn(managed1);
      org.mockito.Mockito.when(entityManager.getReference(Interest.class, id2))
          .thenReturn(managed2);
      org.mockito.Mockito.when(entityManager.getReference(Interest.class, id3))
          .thenReturn(managed3);

      Map<Article, Set<Interest>> map = new LinkedHashMap<>();
      map.put(a1, new LinkedHashSet<>(List.of(raw1, raw2)));
      map.put(a2, new LinkedHashSet<>(List.of(raw3)));

      // when
      articleScrapePersistenceService.saveAll(map);

      // then
      verify(entityManager).getReference(Interest.class, id1);
      verify(entityManager).getReference(Interest.class, id2);
      verify(entityManager).getReference(Interest.class, id3);

      ArgumentCaptor<List<Article>> captor = ArgumentCaptor.forClass(List.class);
      verify(articleRepository).saveAll(captor.capture());
      List<Article> savedArticles = captor.getValue();
      assertEquals(2, savedArticles.size());
      assertSame(a1, savedArticles.get(0));
      assertSame(a2, savedArticles.get(1));

      List<Interest> a1Mapped = a1.getArticleInterests().stream()
          .map(ArticleInterest::getInterest)
          .toList();
      List<Interest> a2Mapped = a2.getArticleInterests().stream()
          .map(ArticleInterest::getInterest)
          .toList();

      assertEquals(2, a1Mapped.size());
      assertEquals(1, a2Mapped.size());
      assertSame(managed1, a1Mapped.get(0));
      assertSame(managed2, a1Mapped.get(1));
      assertSame(managed3, a2Mapped.get(0));
    }

    @Test
    @DisplayName("null interest 또는 id null interest는 스킵")
    void skip_null_or_null_id_interest() {
      // given
      Article a1 = article("https://a.com/1", "a1");
      UUID validId = UUID.randomUUID();

      Interest valid = interestWithId("valid", validId);
      Interest noId = interestWithoutId("no-id");
      Interest managed = interestWithId("managed", validId);

      org.mockito.Mockito.when(entityManager.getReference(Interest.class, validId))
          .thenReturn(managed);

      Set<Interest> interests = new LinkedHashSet<>();
      interests.add(null);
      interests.add(noId);
      interests.add(valid);

      Map<Article, Set<Interest>> map = new LinkedHashMap<>();
      map.put(a1, interests);

      // when
      articleScrapePersistenceService.saveAll(map);

      // then
      verify(entityManager).getReference(Interest.class, validId);
      verify(entityManager, never()).getReference(any(), org.mockito.ArgumentMatchers.isNull());

      assertEquals(1, a1.getArticleInterests().size());
      assertSame(managed, a1.getArticleInterests().get(0).getInterest());
      verify(articleRepository).saveAll(any());
    }

    @Test
    @DisplayName("관심사가 비어 있어도 article saveAll은 호출")
    void return_without_save_when_interests_empty() {
      // given
      Article a1 = article("https://a.com/1", "a1");

      Map<Article, Set<Interest>> map = new LinkedHashMap<>();
      map.put(a1, new LinkedHashSet<>());

      // when
      articleScrapePersistenceService.saveAll(map);

      // then
      verify(entityManager, never()).getReference(any(), any());
      verify(articleRepository, never()).saveAll(any());
      assertEquals(0, a1.getArticleInterests().size());
    }

    @Test
    @DisplayName("input map empty -> return without save")
    void return_without_save_when_input_map_empty() {
      Map<Article, Set<Interest>> map = new LinkedHashMap<>();

      articleScrapePersistenceService.saveAll(map);

      verify(entityManager, never()).getReference(any(), any());
      verify(articleRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("interest set이 비어있지 않아도 유효 id가 없으면 저장하지 않고 반환")
    void return_without_save_when_no_valid_interest_id_exists() {
      // given
      Article a1 = article("https://a.com/1", "a1");
      Interest noId = interestWithoutId("no-id");

      Set<Interest> interests = new LinkedHashSet<>();
      interests.add(null);
      interests.add(noId);

      Map<Article, Set<Interest>> map = new LinkedHashMap<>();
      map.put(a1, interests);

      // when
      articleScrapePersistenceService.saveAll(map);

      // then
      verify(entityManager, never()).getReference(any(), any());
      verify(articleRepository, never()).saveAll(any());
      assertEquals(0, a1.getArticleInterests().size());
    }

    @Test
    @DisplayName("동일 interest id가 중복되어도 getReference는 1회만 호출")
    void call_get_reference_once_when_interest_ids_are_duplicated() {
      // given
      Article a1 = article("https://a.com/1", "a1");
      UUID sameId = UUID.randomUUID();

      Interest raw1 = interestWithId("raw-1", sameId);
      Interest raw2 = interestWithId("raw-2", sameId);
      Interest managed = interestWithId("managed", sameId);

      org.mockito.Mockito.when(entityManager.getReference(Interest.class, sameId))
          .thenReturn(managed);

      Set<Interest> interests = new LinkedHashSet<>();
      interests.add(raw1);
      interests.add(raw2);

      Map<Article, Set<Interest>> map = new LinkedHashMap<>();
      map.put(a1, interests);

      // when
      articleScrapePersistenceService.saveAll(map);

      // then
      verify(entityManager, times(1)).getReference(Interest.class, sameId);
      verify(articleRepository, times(1)).saveAll(any());
      assertEquals(1, a1.getArticleInterests().size());
      assertSame(managed, a1.getArticleInterests().get(0).getInterest());
    }
  }
}
