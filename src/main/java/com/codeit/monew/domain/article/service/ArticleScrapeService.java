package com.codeit.monew.domain.article.service;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.entity.Keyword;
import com.codeit.monew.domain.interest.repository.KeywordRepository;
import com.codeit.monew.global.exception.MonewException;
import com.codeit.monew.global.exception.article.ArticleScrapeException;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import com.codeit.monew.infra.external.rss.XmlClient;
import com.codeit.monew.infra.external.rss.XmlParser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ArticleScrapeService {

  private final XmlClient xmlClient;
  private final XmlParser xmlParser;
  private final ArticleRepository articleRepository;
  private final KeywordRepository keywordRepository;
  // TODO: NotificationService notificationService;

  private static final int BATCH_SIZE = 500;

  public int scrapeAndSave(NewsSourceUrl source, String query) {
    // 외부 소스(RSS/Naver)로부터 XML 데이터를 가져와서 Article 객체 리스트로 변환.
    List<Article> parsedArticles = runStage("fetch_parse", source, query,
        () -> xmlParser.parse(fetchXml(source, query), source));

    // 수집된 기사가 하나도 없다면(네이버 검색 결과 없음) 0을 반환하고 종료
    if (parsedArticles.isEmpty()) {
      return 0;
    }

    // 수집된 리스트 내에서 URL이 중복되는 기사들을 제거 (메모리 상 중복 제거)
    List<Article> distinctArticles = runStage("deduplicate", source, query,
        () -> deduplicateByUrl(parsedArticles));

    // DB를 조회하여 이미 저장된 URL은 제외하고 새로운 기사만 남김
    List<Article> newArticles = runStage("filter_new", source, query,
        () -> filterNewArticles(distinctArticles));

    // 필터링 후 남은 새 기사가 없다면 0을 반환하고 종료
    if (newArticles.isEmpty()) {
      return 0;
    }

    // 사용자들이 등록한 모든 키워드와 그에 연결된 관심사 정보를 DB에서 로드
    List<Keyword> keywords = runStage("load_keywords", source, query,
        this::loadKeywordsWithInterest);

    // 새 기사들의 텍스트를 분석해 키워드와 매칭하고 기사와 관심사를 연결한 map 생성
    Map<Article, Set<Interest>> articleInterestMap = runStage("map_interests", source, query,
        () -> mapArticlesToInterests(newArticles, keywords));

    // 최종적으로 관심사를 가진 기사들을 DB에 저장하고 알림을 보낸 뒤 저장된 개수를 반환
    return runStage("save", source, query, () -> saveAndNotify(articleInterestMap, source));
  }

  private List<Article> deduplicateByUrl(List<Article> articles) {
    return new ArrayList<>(articles.stream()
        .collect(Collectors.toMap(
            Article::getSourceUrl,
            article -> article,
            (existing, replacement) -> existing,
            LinkedHashMap::new
        )).values());
  }

  private List<Article> filterNewArticles(List<Article> parsedArticles) {
    List<String> allUrls = parsedArticles.stream().map(Article::getSourceUrl).toList();
    Set<String> existingUrls = new HashSet<>();

    for (int i = 0; i < allUrls.size(); i += BATCH_SIZE) {
      List<String> chunk = allUrls.subList(i, Math.min(i + BATCH_SIZE, allUrls.size()));
      existingUrls.addAll(articleRepository.findAllExistingUrlsIn(chunk));
    }

    return parsedArticles.stream()
        .filter(article -> !existingUrls.contains(article.getSourceUrl()))
        .toList();
  }

  private List<Keyword> loadKeywordsWithInterest() {
    return keywordRepository.findAllWithInterest().stream()
        .filter(k -> k.getInterest() != null && k.getInterest().getId() != null)
        .filter(k -> StringUtils.hasText(k.getName()))
        .toList();
  }

  private Map<Article, Set<Interest>> mapArticlesToInterests(List<Article> newArticles,
      List<Keyword> keywords) {
    Map<Article, Set<Interest>> map = new LinkedHashMap<>();

    for (Article article : newArticles) {
      Set<Interest> matchedInterests = findMatchedInterests(article, keywords);
      if (!matchedInterests.isEmpty()) {
        matchedInterests.forEach(article::addInterest); // 양방향 연관관계 편의 메서드
        map.put(article, matchedInterests);
      }
    }
    return map;
  }

  private Set<Interest> findMatchedInterests(Article article, List<Keyword> keywords) {
    if (keywords.isEmpty()) { // 등록된 키워드가 없을 경우 early return
      return Set.of();
    }

    // 기사의 제목과 내용을 합쳐서 검색 대상 텍스트를 만들고 모두 소문자로 변환
    String contentToSearch = (Objects.toString(article.getTitle(), "") + "/" +
        Objects.toString(article.getSummary(), "")).toLowerCase();

    // 키워드 이름을 소문자로 변경 후 위에서 만든 테스트에 포함되어 있는지 확인 후 관심사 추출
    return keywords.stream()
        .filter(k -> contentToSearch.contains(k.getName().trim().toLowerCase()))
        .map(Keyword::getInterest)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  //
  private int saveAndNotify(Map<Article, Set<Interest>> articleInterestMap, NewsSourceUrl source) {
    List<Article> toSave = new ArrayList<>(articleInterestMap.keySet());

    if (!toSave.isEmpty()) {
      articleRepository.saveAll(toSave);
      // TODO: notificationService.publishBulkArticleNotifications(articleInterestMap);
      log.info("[{}] {}건의 새로운 기사가 저장 및 알림 처리되었습니다.", source, toSave.size());
    }
    return toSave.size();
  }

  private String fetchXml(NewsSourceUrl source, String query) {
    return (source == NewsSourceUrl.NAVER) ?
        xmlClient.fetchNaverXml(query) : xmlClient.fetchRssXml(source);
  }

  private <T> T runStage(String stage, NewsSourceUrl source, String query, Supplier<T> supplier) {
    try {
      return supplier.get();
    } catch (MonewException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new ArticleScrapeException(source, query, stage, e);
    }
  }
}
