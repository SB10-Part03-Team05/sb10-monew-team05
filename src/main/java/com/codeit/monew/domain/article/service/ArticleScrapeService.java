package com.codeit.monew.domain.article.service;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.interest.entity.Keyword;
import com.codeit.monew.domain.interest.repository.KeywordRepository;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import com.codeit.monew.infra.external.rss.XmlClient;
import com.codeit.monew.infra.external.rss.XmlParser;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ArticleScrapeService {

  private final XmlClient xmlClient;
  private final XmlParser xmlParser;
  private final ArticleRepository articleRepository;
  private final KeywordRepository keywordRepository;

  public int scrapeAndSave(NewsSourceUrl source, String query) {
    // XML 데이터 가져오기 및 파싱
    String xml = fetchXml(source, query);
    List<Article> parsed = xmlParser.parse(xml, source);

    // 필터링에 필요한 키워드 셋 준비
    Set<String> keywords = loadKeywords();

    // todo: 키워드 필터를 통과한 기사마다 existsBySourceUrl을 호출하는 N+1 문제 개선 예정
    // 키워드 필터링 및 중복 제거
    List<Article> articlesToSave = parsed.stream()
        // 키워드가 포함된 기사만 필터링
        .filter(article -> matchesKeyword(article, keywords))
        // 이미 저장된 URL이 아닌 기사만 필터링
        .filter(article -> !articleRepository.existsBySourceUrl(article.getSourceUrl()))
        // 결과 수집
        .toList();

    // 4. 한꺼번에 저장
    articleRepository.saveAll(articlesToSave);

    // 5. 저장된 개수 반환
    return articlesToSave.size();
  }

  private String fetchXml(NewsSourceUrl source, String query) {
    if (source == NewsSourceUrl.NAVER) {
      return xmlClient.fetchNaverXml(query);
    }
    return xmlClient.fetchRssXml(source);
  }

  private Set<String> loadKeywords() {
    return keywordRepository.findAll().stream()
        .map(Keyword::getName)
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .collect(Collectors.toSet());
  }

  private boolean matchesKeyword(Article article, Set<String> keywords) {
    if (keywords.isEmpty()) {
      return false; // 등록된 키워드가 없다면 early return
    }

    // 제목과 요약을 합친 후 소문자로 변환
    String contentToSearch = (Objects.toString(article.getTitle(), "") +
        Objects.toString(article.getSummary(), "")).toLowerCase();

    // 키워드도 소문자로 변환하여 비교(대소문자 상관없이 매칭되기 위해서)
    return keywords.stream()
        .map(String::toLowerCase)
        .anyMatch(contentToSearch::contains);
  }
}