package com.codeit.monew.infra.external.rss;

import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.global.exception.article.InvalidArticleEntityException;
import com.codeit.monew.global.exception.external.parser.EmptyXmlInputException;
import com.codeit.monew.global.exception.external.parser.ExternalInvalidXmlException;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class XmlParser {

  private static final String DEFAULT_SUMMARY = "요약이 제공되지 않는 출처입니다";
  private static final int CRAWLED_SUMMARY_MAX_LENGTH = 220;

  private final ArticleBodyCrawler articleBodyCrawler;

  public List<Article> parse(String xml, NewsSourceUrl source) {
    // xml이 비어있다면 예외 발생
    if (xml == null || xml.isBlank()) {
      throw new EmptyXmlInputException(source);
    }

    // XML 문법에 어긋나는 요소들을 정규표현식으로 미리 제거 (전처리)
    String sanitizedXml = xml
        .replaceAll("<!DOCTYPE[^>]*>", "")
        // MK 등 일부 RSS의 pubDate 오프셋(+09:00)을 ROME 호환(+0900)으로 정규화
        .replaceAll("(?i)(<pubDate>\\s*[^<]*[+-]\\d{2}):(\\d{2}\\s*</pubDate>)", "$1$2");

    try {
      // ROME 라이브러리를 사용하여 문자열 XML을 SyndFeed(RSS 표준 객체)로 변환
      SyndFeed feed = new SyndFeedInput().build(new StringReader(sanitizedXml));
      List<Article> articles = new ArrayList<>();
      int skippedInvalid = 0;
      int skippedUnexpected = 0;

      // 피드 안의 개별 기사 항목(SyndEntry)을 하나씩 순회
      for (SyndEntry entry : feed.getEntries()) {
        try {
          String link = extractLink(entry, source);
          Article article = Article.createArticle(
              source.getArticleSource(),
              link,
              entry.getTitle() == null ? null : Jsoup.parse(entry.getTitle()).text().trim(),
              entry.getPublishedDate() == null ? null : entry.getPublishedDate().toInstant(),
              extractSummary(entry, source, link)
          );
          articles.add(article);
          log.debug("Article Source: {}, URL: {}, Title: {}, Published Date: {}, Summary: {}",
              article.getSource(), article.getSourceUrl(), article.getTitle(),
              article.getPublishDate(), article.getSummary());
        } catch (InvalidArticleEntityException e) { // 특정 엔트리의 엔티티 무결성이 잘못된 경우 해당 엔트리 스킵
          skippedInvalid++;
          log.warn("[{}] entry parse skipped(invalid): title={}, link={}, details={}",
              source, entry.getTitle(), entry.getLink(), e.getDetails());
        } catch (Exception e) { // 알 수 없는 예외로 특정 엔트리의 파싱이 실패한 경우 해당 엔트리 스킵
          skippedUnexpected++;
          log.error(
              "[{}] entry parse skipped(unexpected): title={}, link={}, errorType={}, message={}",
              source, entry.getTitle(), entry.getLink(), e.getClass().getSimpleName(),
              e.getMessage(), e);
        }
      }

      log.info("[{}] parse finished. total={}, parsed={}, skippedInvalid={}, skippedUnexpected={}",
          source, feed.getEntries().size(), articles.size(), skippedInvalid, skippedUnexpected);
      return articles;
    } catch (FeedException e) {
      throw new ExternalInvalidXmlException(source, e);
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

  private String extractSummary(SyndEntry entry, NewsSourceUrl source, String sourceUrl) {
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

    // 3. RSS에 데이터가 전혀 없다면 크롤러에게 위임
    String crawledBodyText = articleBodyCrawler.crawlBodyText(sourceUrl, source);
    return StringUtils.hasText(crawledBodyText)
        ? toSummaryCandidate(crawledBodyText)
        : DEFAULT_SUMMARY; // 크롤링 된 값이 없다면(또는 크롤링에 실패했다면) 기본 문구 반환
  }

  // TODO: 문자열 자르기가 아닌 AI 요약으로 변경 할 예정
  private String toSummaryCandidate(String bodyText) {
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    if (bodyText.length() <= CRAWLED_SUMMARY_MAX_LENGTH) {
      return bodyText;
    }
    return bodyText.substring(0, CRAWLED_SUMMARY_MAX_LENGTH).trim() + "...";
  }
}
