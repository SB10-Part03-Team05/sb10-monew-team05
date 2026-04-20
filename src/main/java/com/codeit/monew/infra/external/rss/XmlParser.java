package com.codeit.monew.infra.external.rss;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.entity.Article;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class XmlParser {

  public List<Article> parse(String xml, NewsSourceUrl source) {
    // xml이 비어있다면 early return
    if (xml == null || xml.isBlank()) {
      return List.of();
    }

    // XML 문법에 어긋나는 요소들을 정규표현식으로 미리 제거 (전처리)
    String sanitizedXml = xml
        .replaceAll("<!DOCTYPE[^>]*>", "");

    try {
      // ROME 라이브러리를 사용하여 문자열 XML을 SyndFeed(RSS 표준 객체)로 변환
      SyndFeed feed = new SyndFeedInput().build(new StringReader(sanitizedXml));
      List<Article> articles = new ArrayList<>();

      // 피드 안의 개별 기사 항목(SyndEntry)을 하나씩 순회
      for (SyndEntry entry : feed.getEntries()) {
        try {
          articles.add(Article.createArticle(
              mapArticleSource(source),
              extractLink(entry, source),
              entry.getTitle() == null ? null : Jsoup.parse(entry.getTitle()).text().trim(),
              entry.getPublishedDate() == null ? null
                  : entry.getPublishedDate().toInstant(),
              extractSummary(entry)
          ));
        } catch (IllegalArgumentException e) {
          // 파싱 중간에 잘못된 값이 필터링 되지 않은 경우 스킵하고 남은 기사 항목(SyndEntry) 파싱 진행
          log.warn("[{}] entry parse skipped: {}", source, e.getMessage());
        }
      }

      return articles;
    } catch (FeedException e) {
      log.error("[{}] XML parse failed: {}", source, e.getMessage());
      return List.of();
    }
  }

  private String extractLink(SyndEntry entry, NewsSourceUrl source) {
    if (source == NewsSourceUrl.NAVER) {
      return entry.getForeignMarkup().stream()
          .filter(el -> "originallink".equals(el.getName()))
          .findFirst()
          .map(org.jdom2.Element::getValue)
          .orElse(entry.getLink());
    }
    return entry.getLink();
  }

  private String extractSummary(SyndEntry entry) {
    // Description이 있는 경우 -> 네이버, 연합뉴스
    if (entry.getDescription() != null) {
      String desc = Jsoup.parse(entry.getDescription().getValue()).text();
      if (StringUtils.hasText(desc)) {
        return desc;
      }
    }

    // Description이 없고 Content가 있는 경우 -> 조선일보
    if (!entry.getContents().isEmpty()) {
      String encoded = Jsoup.parse(entry.getContents().get(0).getValue()).text();
      if (StringUtils.hasText(encoded)) {
        return encoded;
      }
    }

    // Description이 없고, Content도 없는 경우 -> 한국경제
    // todo: 심화) 나중에 원문 링크 접속해서 body 크롤링해서 Gemini API로 요약 제공
    return "요약이 제공되지 않는 출처입니다";
  }

  private ArticleSource mapArticleSource(NewsSourceUrl source) {
    return switch (source) {
      case NAVER -> ArticleSource.NAVER;
      case HANKYUNG -> ArticleSource.HANKYUNG;
      case CHOSUN -> ArticleSource.CHOSUN;
      case YONHAP -> ArticleSource.YONHAP;
    };
  }
}

