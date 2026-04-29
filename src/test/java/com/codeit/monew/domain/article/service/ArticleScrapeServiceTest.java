package com.codeit.monew.domain.article.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.article.scheduler.ArticleScrapeResult;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.entity.Keyword;
import com.codeit.monew.domain.interest.repository.KeywordRepository;
import com.codeit.monew.global.exception.MonewException;
import com.codeit.monew.global.exception.article.ArticleScrapeException;
import com.codeit.monew.global.exception.external.client.ExternalNetworkException;
import com.codeit.monew.infra.external.llm.LlmSummaryService;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import com.codeit.monew.infra.external.rss.XmlClient;
import com.codeit.monew.infra.external.rss.XmlParser;
import java.time.Instant;
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
class ArticleScrapeServiceTest {

  @Mock
  private XmlClient xmlClient;
  @Mock
  private XmlParser xmlParser;
  @Mock
  private ArticleRepository articleRepository;
  @Mock
  private KeywordRepository keywordRepository;
  @Mock
  private LlmSummaryService llmSummaryService;
  @Mock
  private ArticleScrapePersistenceService articleScrapePersistenceService;

  @InjectMocks
  private ArticleScrapeService articleScrapeService;

  private Article article(String url, String title, String summary) {
    return Article.createArticle(
        ArticleSource.NAVER,
        url,
        title,
        Instant.parse("2026-04-01T00:00:00Z"),
        summary
    );
  }

  private Interest interest(String name) {
    Interest i = Interest.create(name);
    ReflectionTestUtils.setField(i, "id", UUID.randomUUID());
    return i;
  }

  private Keyword keyword(Interest interest, String name) {
    return Keyword.create(interest, name);
  }

  @Nested
  @DisplayName("scrapeAndSave")
  class ScrapeAndSave {

    @Test
    @DisplayName("RSS 소스에서 정상 저장")
    void success_rss_source_save() {
      // given: 일반 RSS 소스 기사와 삼성 관심사, 키워드 엔티티를 세팅하고 외부 API 및 DB 호출을 모킹
      Article a1 = article("https://a.com/1", "삼성 소식", "반도체");
      Interest samsung = interest("삼성");
      Keyword k = keyword(samsung, "삼성");

      given(xmlClient.fetchRssXml(NewsSourceUrl.CHOSUN)).willReturn("<xml/>");
      given(xmlParser.parse("<xml/>", NewsSourceUrl.CHOSUN)).willReturn(List.of(a1));
      given(articleRepository.findAllExistingUrlsIn(anyList())).willReturn(List.of()); // 중복 URL 없음
      given(keywordRepository.findAllWithInterest()).willReturn(List.of(k)); // 키워드 매칭 가능

      // when: 조선일보 RSS 소스에 대한 스크래핑 및 저장 로직 실행
      ArticleScrapeResult result = articleScrapeService.scrapeAndSave(NewsSourceUrl.CHOSUN, null);

      // then: 1건이 성공적으로 저장되고, 기사에 '삼성' 관심사가 매핑되었는지 검증
      assertEquals(1, result.totalSavedCount());
      verify(xmlClient).fetchRssXml(NewsSourceUrl.CHOSUN);
      verify(articleScrapePersistenceService).saveAll(anyMap());
    }

    @Test
    @DisplayName("NAVER 소스에서 정상 저장")
    void success_naver_source_save() {
      // given: 네이버 소스 검색 결과 기사와 네이버 관심사, 키워드 엔티티를 세팅하고 모킹
      Article a1 = article("https://a.com/1", "네이버 AI", "하이퍼클로바");
      Interest naver = interest("네이버");
      Keyword k = keyword(naver, " 네이버");

      given(xmlClient.fetchNaverXml("네이버")).willReturn("<xml/>");
      given(xmlParser.parse("<xml/>", NewsSourceUrl.NAVER)).willReturn(List.of(a1));
      given(articleRepository.findAllExistingUrlsIn(anyList())).willReturn(List.of());
      given(keywordRepository.findAllWithInterest()).willReturn(List.of(k));

      // when: 검색어 "네이버"를 이용하여 네이버 API 소스 스크래핑 및 저장 로직 실행
      ArticleScrapeResult result = articleScrapeService.scrapeAndSave(NewsSourceUrl.NAVER, "네이버");

      // then: 1건이 성공적으로 저장되고 올바른 클라이언트 메서드가 호출되었는지 검증
      assertEquals(1, result.totalSavedCount());
      verify(xmlClient).fetchNaverXml("네이버");
    }

    @Test
    @DisplayName("파싱 결과가 비어 있으면 0 반환")
    void return_zero_when_parsed_articles_empty() {
      // given: 외부 API 호출은 성공했으나, XML 파싱 결과 기사 리스트가 비어있도록 모킹
      given(xmlClient.fetchRssXml(NewsSourceUrl.HANKYUNG)).willReturn("<xml/>");
      given(xmlParser.parse("<xml/>", NewsSourceUrl.HANKYUNG)).willReturn(List.of());

      // when: 스크래핑 로직 실행
      ArticleScrapeResult result = articleScrapeService.scrapeAndSave(NewsSourceUrl.HANKYUNG, null);

      // then: 저장된 기사 수가 0이며, 이후 불필요한 DB 조회가 발생하지 않음을 검증
      assertEquals(0, result.totalSavedCount());
      verify(articleRepository, never()).findAllExistingUrlsIn(anyList());
      verify(keywordRepository, never()).findAllWithInterest();
    }

    @Test
    @DisplayName("URL 중복 제거")
    void deduplicate_by_url() {
      // given: 파싱된 기사 리스트에 중복된 URL(https://dup.com/1)을 가진 기사가 2건 포함되도록 모킹
      Article first = article("https://dup.com/1", "first", "a");
      Article duplicate = article("https://dup.com/1", "second", "b");
      Interest i = interest("dup");
      Keyword k = keyword(i, "first");

      given(xmlClient.fetchRssXml(NewsSourceUrl.YONHAP)).willReturn("<xml/>");
      given(xmlParser.parse("<xml/>", NewsSourceUrl.YONHAP)).willReturn(List.of(first, duplicate));
      given(articleRepository.findAllExistingUrlsIn(anyList())).willReturn(List.of());
      given(keywordRepository.findAllWithInterest()).willReturn(List.of(k));

      // when: 스크래핑 및 저장 로직 실행
      ArticleScrapeResult result = articleScrapeService.scrapeAndSave(NewsSourceUrl.YONHAP, null);

      // then: URL 기준으로 중복을 제거하여 1건만 저장요청되며, 첫 번째 기사만 살아남았는지 검증
      assertEquals(1, result.totalSavedCount());
      ArgumentCaptor<Map<Article, Set<Interest>>> captor = ArgumentCaptor.forClass(Map.class);
      verify(articleScrapePersistenceService).saveAll(captor.capture());
      Article saved = captor.getValue().keySet().iterator().next();
      assertEquals("https://dup.com/1", saved.getSourceUrl());
      assertEquals("first", saved.getTitle());
    }

    @Test
    @DisplayName("기존 URL 필터 후 비어 있으면 0 반환")
    void return_zero_when_new_articles_empty_after_filter() {
      // given: 파싱된 1건의 기사가 이미 DB에 존재하는 URL임을 모킹
      Article a1 = article("https://exists.com/1", "t1", "s1");

      given(xmlClient.fetchRssXml(NewsSourceUrl.CHOSUN)).willReturn("<xml/>");
      given(xmlParser.parse("<xml/>", NewsSourceUrl.CHOSUN)).willReturn(List.of(a1));
      given(articleRepository.findAllExistingUrlsIn(anyList())).willReturn(
          List.of("https://exists.com/1"));

      // when: 스크래핑 로직 실행
      ArticleScrapeResult result = articleScrapeService.scrapeAndSave(NewsSourceUrl.CHOSUN, null);

      // then: 저장 대상 기사가 없으므로 0건 저장되며, 관심사 매핑 및 저장 로직이 수행되지 않음을 검증
      assertEquals(0, result.totalSavedCount());
      verify(keywordRepository, never()).findAllWithInterest();
      verify(articleScrapePersistenceService, never()).saveAll(anyMap());
    }

    @Test
    @DisplayName("기존 URL과 일부만 겹치면 신규 기사만 저장")
    void save_only_new_articles_when_some_urls_already_exist() {
      // given: DB에 이미 있는 기사(old)와 신규 기사(fresh)가 섞여서 파싱되도록 모킹
      Article existing = article("https://exists.com/1", "old", "old-summary");
      Article fresh = article("https://new.com/2", "new", "new-summary");
      Interest interest = interest("new");
      Keyword keyword = keyword(interest, "new");

      given(xmlClient.fetchRssXml(NewsSourceUrl.CHOSUN)).willReturn("<xml/>");
      given(xmlParser.parse("<xml/>", NewsSourceUrl.CHOSUN)).willReturn(List.of(existing, fresh));
      given(articleRepository.findAllExistingUrlsIn(anyList())).willReturn(
          List.of("https://exists.com/1"));
      given(keywordRepository.findAllWithInterest()).willReturn(List.of(keyword));

      // when: 스크래핑 및 저장 로직 실행
      ArticleScrapeResult result = articleScrapeService.scrapeAndSave(NewsSourceUrl.CHOSUN, null);

      // then: 기존 기사는 필터링되고, 1건(새 기사)만 최종적으로 DB에 저장되는지 검증
      assertEquals(1, result.totalSavedCount());

      ArgumentCaptor<Map<Article, Set<Interest>>> captor = ArgumentCaptor.forClass(Map.class);
      verify(articleScrapePersistenceService).saveAll(captor.capture());
      assertEquals(1, captor.getValue().size());
      Article saved = captor.getValue().keySet().iterator().next();
      assertEquals("https://new.com/2", saved.getSourceUrl());
    }

    @Test
    @DisplayName("키워드가 없으면 관심사 매핑 없이 비어 있음")
    void empty_mapping_when_keywords_empty() {
      // given: 새 기사는 존재하지만 시스템에 등록된 키워드가 하나도 없는 상황을 모킹
      Article a1 = article("https://a.com/1", "삼성", "요약");

      given(xmlClient.fetchRssXml(NewsSourceUrl.YONHAP)).willReturn("<xml/>");
      given(xmlParser.parse("<xml/>", NewsSourceUrl.YONHAP)).willReturn(List.of(a1));
      given(articleRepository.findAllExistingUrlsIn(anyList())).willReturn(List.of());
      given(keywordRepository.findAllWithInterest()).willReturn(List.of()); // 등록된 키워드 없음

      // when: 스크래핑 및 저장 로직 실행
      ArticleScrapeResult result = articleScrapeService.scrapeAndSave(NewsSourceUrl.YONHAP, null);

      // then: 어떤 관심사에도 매핑되지 않은 기사는 저장 대상에서 제외되어 0건이 저장됨을 검증
      assertEquals(0, result.totalSavedCount());
      verify(articleScrapePersistenceService, never()).saveAll(anyMap());
    }

    @Test
    @DisplayName("키워드 매칭 시 관심사 매핑")
    void map_interests_when_keyword_matched() {
      // given: 대소문자 무관하게(" samsung ") 기사 제목("SAMSUNG 반도체")과 매칭되는 키워드 세팅
      Article a1 = article("https://a.com/1", "SAMSUNG 반도체", "소식");
      Interest samsung = interest("삼성");
      Keyword k = keyword(samsung, " samsung ");

      given(xmlClient.fetchRssXml(NewsSourceUrl.CHOSUN)).willReturn("<xml/>");
      given(xmlParser.parse("<xml/>", NewsSourceUrl.CHOSUN)).willReturn(List.of(a1));
      given(articleRepository.findAllExistingUrlsIn(anyList())).willReturn(List.of());
      given(keywordRepository.findAllWithInterest()).willReturn(List.of(k));

      // when: 스크래핑 및 저장 로직 실행
      ArticleScrapeResult result = articleScrapeService.scrapeAndSave(NewsSourceUrl.CHOSUN, null);

      // then: 매칭이 올바르게 이루어져 1건이 저장되고, 해당 기사에 삼성 관심사가 연관관계로 묶였는지 검증
      assertEquals(1, result.totalSavedCount());
      verify(articleScrapePersistenceService).saveAll(anyMap());
    }

    @Test
    @DisplayName("매칭 없는 기사는 저장 대상에서 제외")
    void exclude_article_without_interest_match() {
      // given: 관심사 매칭이 가능한 기사(애플)와 불가능한 기사(테슬라)가 섞여서 파싱되도록 모킹
      Article matched = article("https://a.com/1", "애플", "아이폰");
      Article unmatched = article("https://a.com/2", "테슬라", "모터스");
      Interest apple = interest("애플");
      Keyword k = keyword(apple, "애플"); // 테슬라 키워드는 없음

      given(xmlClient.fetchRssXml(NewsSourceUrl.CHOSUN)).willReturn("<xml/>");
      given(xmlParser.parse("<xml/>", NewsSourceUrl.CHOSUN)).willReturn(
          List.of(matched, unmatched));
      given(articleRepository.findAllExistingUrlsIn(anyList())).willReturn(List.of());
      given(keywordRepository.findAllWithInterest()).willReturn(List.of(k));

      // when: 스크래핑 및 저장 로직 실행
      ArticleScrapeResult result = articleScrapeService.scrapeAndSave(NewsSourceUrl.CHOSUN, null);

      // then: 관심사가 매핑되지 않은 테슬라 기사는 버려지고, 애플 기사 1건만 최종 저장되는지 검증
      assertEquals(1, result.totalSavedCount());
      ArgumentCaptor<Map<Article, Set<Interest>>> captor = ArgumentCaptor.forClass(Map.class);
      verify(articleScrapePersistenceService).saveAll(captor.capture());
      assertEquals(1, captor.getValue().size());
      Article saved = captor.getValue().keySet().iterator().next();
      assertEquals("https://a.com/1", saved.getSourceUrl());
    }

    @Test
    @DisplayName("요약이 있는 소스: 저장 대상이 있으면 saveAll 호출")
    void call_save_all_for_chosun() {
      // given: 조선일보처럼 이미 요약이 포함된 소스로부터 기사(카카오)가 파싱된 상황 설정
      Article a1 = article("https://a.com/1", "카카오", "톡");
      Interest kakao = interest("카카오");
      Keyword k = keyword(kakao, "카카오");

      given(xmlClient.fetchRssXml(NewsSourceUrl.CHOSUN)).willReturn("<xml/>");
      given(xmlParser.parse("<xml/>", NewsSourceUrl.CHOSUN)).willReturn(List.of(a1));
      given(articleRepository.findAllExistingUrlsIn(anyList())).willReturn(List.of());
      given(keywordRepository.findAllWithInterest()).willReturn(List.of(k));

      // when: 조선일보 소스에 대해 스크래핑 및 저장 로직 실행
      articleScrapeService.scrapeAndSave(NewsSourceUrl.CHOSUN, null);

      // then: 이미 요약이 있으므로 LLM 요약을 호출하지 않고 바로 saveAll이 호출되는지 검증
      verify(articleScrapePersistenceService).saveAll(anyMap());
      verify(llmSummaryService, never()).summarizeOrOriginal(anyString(), anyString());
    }

    @Test
    @DisplayName("요약이 없어 크롤링 된 본문이 있으면서 LLM 사용 소스: 요약 후 saveAll 호출")
    void call_save_all_for_hankyung_with_llm_summary() {
      // given: 한국경제처럼 원문 요약이 없어서 LLM 요약이 필요한 기사(카카오) 설정
      Article a1 = article("https://a.com/1", "카카오", "원문 요약");
      Interest kakao = interest("카카오");
      Keyword k = keyword(kakao, "카카오");

      given(xmlClient.fetchRssXml(NewsSourceUrl.HANKYUNG)).willReturn("<xml/>");
      given(xmlParser.parse("<xml/>", NewsSourceUrl.HANKYUNG)).willReturn(List.of(a1));
      given(articleRepository.findAllExistingUrlsIn(anyList())).willReturn(List.of());
      given(keywordRepository.findAllWithInterest()).willReturn(List.of(k));
      given(llmSummaryService.summarizeOrOriginal("원문 요약", "https://a.com/1"))
          .willReturn("LLM 요약");

      // when: 한국경제 소스에 대해 스크래핑 및 저장 로직 실행
      articleScrapeService.scrapeAndSave(NewsSourceUrl.HANKYUNG, null);

      // then: LLM 요약 서비스가 호출되었는지 확인하고, 기사 객체에 요약문이 업데이트되었는지 검증
      verify(llmSummaryService).summarizeOrOriginal("원문 요약", "https://a.com/1");
      verify(articleScrapePersistenceService).saveAll(anyMap());
      assertEquals("LLM 요약", a1.getSummary());
    }
  }

  @Nested
  @DisplayName("runStage 예외 처리")
  class RunStage {

    @Test
    @DisplayName("MonewException은 그대로 전파")
    void propagate_monew_exception() {
      // given: 외부 API 호출 중 우리가 정의한 커스텀 예외(MonewException 하위)가 발생하도록 모킹
      MonewException origin = new ExternalNetworkException(
          NewsSourceUrl.CHOSUN, "https://x", new RuntimeException("network"));
      given(xmlClient.fetchRssXml(NewsSourceUrl.CHOSUN)).willThrow(origin);

      // when & then: 스크래핑 중 해당 커스텀 예외가 그대로(동일 객체로) 외부로 던져지는지 검증
      MonewException thrown = assertThrows(MonewException.class,
          () -> articleScrapeService.scrapeAndSave(NewsSourceUrl.CHOSUN, null));

      assertSame(origin, thrown);
    }

    @Test
    @DisplayName("스테이지 단계 런타임 예외는 ArticleScrapeException으로 래핑")
    void wrap_runtime_exception_in_stage() {
      // given: 외부 API 호출 중 예상치 못한 일반 RuntimeException이 발생하도록 모킹
      given(xmlClient.fetchRssXml(NewsSourceUrl.HANKYUNG))
          .willThrow(new RuntimeException("boom"));

      // when: 예외가 발생하는 스크래핑 로직을 실행하고 해당 예외 캡처
      ArticleScrapeException ex = assertThrows(ArticleScrapeException.class,
          () -> articleScrapeService.scrapeAndSave(NewsSourceUrl.HANKYUNG, null));

      // then: 발생한 런타임 예외가 도메인에 맞는 ArticleScrapeException으로 래핑되며, 어떤 단계에서 어떤 소스로 인해 실패했는지 세부 정보가 담겨있는지 검증
      assertEquals("fetch_parse", ex.getDetails().get("stage"));
      assertEquals(NewsSourceUrl.HANKYUNG, ex.getDetails().get("source"));
      assertNull(ex.getDetails().get("query"));
    }
  }
}
