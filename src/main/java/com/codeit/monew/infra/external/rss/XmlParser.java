package com.codeit.monew.infra.external.rss;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.global.exception.article.InvalidArticleEntityException;
import com.codeit.monew.global.exception.external.ExternalInvalidXmlException;
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
    // xml이 비어있다면 예외 발생
    if (xml == null || xml.isBlank()) {
      throw new ExternalInvalidXmlException(
          source,
          "empty_or_blank_xml",
          xml == null ? null : xml.length()
      );
    }

    // XML 문법에 어긋나는 요소들을 정규표현식으로 미리 제거 (전처리)
    String sanitizedXml = xml
        .replaceAll("<!DOCTYPE[^>]*>", "");

    try {
      // ROME 라이브러리를 사용하여 문자열 XML을 SyndFeed(RSS 표준 객체)로 변환
      SyndFeed feed = new SyndFeedInput().build(new StringReader(sanitizedXml));
      List<Article> articles = new ArrayList<>();
      int skippedInvalid = 0;
      int skippedUnexpected = 0;

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
      throw new ExternalInvalidXmlException(source, "feed_parse_failed", e);
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
