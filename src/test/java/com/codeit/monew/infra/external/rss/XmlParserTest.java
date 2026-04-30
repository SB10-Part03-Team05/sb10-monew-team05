package com.codeit.monew.infra.external.rss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.global.exception.external.parser.EmptyXmlInputException;
import com.codeit.monew.global.exception.external.parser.ExternalInvalidXmlException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class XmlParserTest {

  @Mock
  private ArticleBodyCrawler articleBodyCrawler;

  private XmlParser xmlParser;

  @BeforeEach
  void setUp() {
    xmlParser = new XmlParser(articleBodyCrawler);
  }

  private String rssXml(String... items) {
    StringBuilder sb = new StringBuilder();
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    sb.append("<rss version=\"2.0\"><channel><title>test</title>");
    for (String item : items) {
      sb.append(item);
    }
    sb.append("</channel></rss>");
    return sb.toString();
  }

  private String item(
      String title,
      String link,
      String pubDate,
      String description,
      String extraTag
  ) {
    StringBuilder sb = new StringBuilder();
    sb.append("<item>");
    if (title != null) {
      sb.append("<title>").append(title).append("</title>");
    }
    if (link != null) {
      sb.append("<link>").append(link).append("</link>");
    }
    if (pubDate != null) {
      sb.append("<pubDate>").append(pubDate).append("</pubDate>");
    }
    if (description != null) {
      sb.append("<description>").append(description).append("</description>");
    }
    if (extraTag != null) {
      sb.append(extraTag);
    }
    sb.append("</item>");
    return sb.toString();
  }

  @Nested
  @DisplayName("parse")
  class Parse {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("null/blank 입력이면 EmptyXmlInputException(empty_or_blank_xml)")
    void throw_when_invalid_input(String xml) {
      EmptyXmlInputException ex = assertThrows(EmptyXmlInputException.class,
          () -> xmlParser.parse(xml, NewsSourceUrl.CHOSUN));

      assertEquals("empty_or_blank_xml", ex.getDetails().get("reason"));
    }

    @Test
    @DisplayName("XML 포맷이 잘못되면 ExternalInvalidXmlException(reason : invalid_xml)")
    void throw_when_feed_parse_failed() {
      ExternalInvalidXmlException ex = assertThrows(ExternalInvalidXmlException.class,
          () -> xmlParser.parse("<not-xml>", NewsSourceUrl.CHOSUN));

      assertEquals("invalid_xml", ex.getDetails().get("reason"));
    }

    @Test
    @DisplayName("정상 RSS 파싱")
    void success_parse_rss() {
      String xml = rssXml(item(
          "기사 제목",
          "https://example.com/a",
          "Mon, 21 Apr 2026 09:00:00 GMT",
          "<b>요약</b>",
          null
      ));

      List<Article> result = xmlParser.parse(xml, NewsSourceUrl.CHOSUN);

      assertEquals(1, result.size());
      assertEquals("기사 제목", result.get(0).getTitle());
      assertEquals("https://example.com/a", result.get(0).getSourceUrl());
      assertEquals("요약", result.get(0).getSummary());
      verifyNoInteractions(articleBodyCrawler);
    }

    @Test
    @DisplayName("NAVER originallink 우선 사용")
    void use_originallink_for_naver() {
      String xml = rssXml(item(
          "네이버 기사",
          "https://naver.com/link",
          "Mon, 21 Apr 2026 09:00:00 GMT",
          "요약",
          "<n:originallink xmlns:n=\"urn:naver\">https://origin.com/real</n:originallink>"
      ));

      List<Article> result = xmlParser.parse(xml, NewsSourceUrl.NAVER);

      assertEquals(1, result.size());
      assertEquals("https://origin.com/real", result.get(0).getSourceUrl());
    }

    @Test
    @DisplayName("NAVER originallink 없으면 entry.link 사용")
    void fallback_to_entry_link_for_naver() {
      String xml = rssXml(item(
          "네이버 기사",
          "https://naver.com/link",
          "Mon, 21 Apr 2026 09:00:00 GMT",
          "요약",
          null
      ));

      List<Article> result = xmlParser.parse(xml, NewsSourceUrl.NAVER);

      assertEquals("https://naver.com/link", result.get(0).getSourceUrl());
    }

    @Test
    @DisplayName("RSS는 entry.link 사용")
    void use_entry_link_for_non_naver() {
      String xml = rssXml(item(
          "조선 기사",
          "https://chosun.com/link",
          "Mon, 21 Apr 2026 09:00:00 GMT",
          "요약",
          "<originallink>https://ignored.com</originallink>"
      ));

      List<Article> result = xmlParser.parse(xml, NewsSourceUrl.CHOSUN);

      assertEquals("https://chosun.com/link", result.get(0).getSourceUrl());
    }

    @Test
    @DisplayName("description 우선으로 summary 추출")
    void summary_from_description_first() {
      String xml = rssXml(item(
          "기사",
          "https://example.com/1",
          "Mon, 21 Apr 2026 09:00:00 GMT",
          "<p>description 텍스트</p>",
          "<content:encoded xmlns:content=\"http://purl.org/rss/1.0/modules/content/\">content 텍스트</content:encoded>"
      ));

      List<Article> result = xmlParser.parse(xml, NewsSourceUrl.CHOSUN);

      assertEquals("description 텍스트", result.get(0).getSummary());
      verifyNoInteractions(articleBodyCrawler);
    }

    @Test
    @DisplayName("description 없고 contents 있으면 contents로 summary 추출")
    void summary_from_contents_when_description_missing() {
      String xml = rssXml(item(
          "기사",
          "https://example.com/2",
          "Mon, 21 Apr 2026 09:00:00 GMT",
          null,
          "<content:encoded xmlns:content=\"http://purl.org/rss/1.0/modules/content/\"><![CDATA[<div>content 본문</div>]]></content:encoded>"
      ));

      List<Article> result = xmlParser.parse(xml, NewsSourceUrl.CHOSUN);

      assertEquals("content 본문", result.get(0).getSummary());
      verifyNoInteractions(articleBodyCrawler);
    }

    @Test
    @DisplayName("HANKYUNG은 본문 크롤링 결과를 summary로 사용")
    void summary_from_crawled_body_for_hankyung() {
      String xml = rssXml(item(
          "기사",
          "https://example.com/3",
          "Mon, 21 Apr 2026 09:00:00 GMT",
          null,
          null
      ));
      given(articleBodyCrawler.crawlBodyText("https://example.com/3", NewsSourceUrl.HANKYUNG))
          .willReturn("크롤링 본문 요약");

      List<Article> result = xmlParser.parse(xml, NewsSourceUrl.HANKYUNG);

      assertEquals("크롤링 본문 요약", result.get(0).getSummary());
    }

    @Test
    @DisplayName("HANKYUNG 크롤링이 비면 기본 요약 문구 사용")
    void summary_fallback_default_message_when_crawled_summary_blank() {
      String xml = rssXml(item(
          "기사",
          "https://example.com/4",
          "Mon, 21 Apr 2026 09:00:00 GMT",
          null,
          null
      ));
      given(articleBodyCrawler.crawlBodyText("https://example.com/4", NewsSourceUrl.HANKYUNG))
          .willReturn(" ");

      List<Article> result = xmlParser.parse(xml, NewsSourceUrl.HANKYUNG);

      assertEquals("요약이 제공되지 않는 출처입니다", result.get(0).getSummary());
    }

    @Test
    @DisplayName("description과 contents 없고 크롤러 지원 소스가 아니면 기본 요약 문구")
    void summary_fallback_default_message() {
      String xml = rssXml(item(
          "기사",
          "https://example.com/5",
          "Mon, 21 Apr 2026 09:00:00 GMT",
          null,
          null
      ));

      List<Article> result = xmlParser.parse(xml, NewsSourceUrl.CHOSUN);

      assertFalse(result.get(0).getSummary().isBlank());
      assertEquals("요약이 제공되지 않는 출처입니다", result.get(0).getSummary());
    }

    @Test
    @DisplayName("유효하지 않은 엔트리는 skip하고 나머지만 파싱")
    void skip_invalid_entry_and_parse_others() {
      String valid = item(
          "정상 기사",
          "https://example.com/ok",
          "Mon, 21 Apr 2026 09:00:00 GMT",
          "요약",
          null
      );
      String invalidNoTitle = item(
          null,
          "https://example.com/bad",
          "Mon, 21 Apr 2026 09:00:00 GMT",
          "요약",
          null
      );

      List<Article> result = xmlParser.parse(rssXml(valid, invalidNoTitle), NewsSourceUrl.CHOSUN);

      assertEquals(1, result.size());
      assertEquals("https://example.com/ok", result.get(0).getSourceUrl());
    }
  }

  @Nested
  @DisplayName("source 매핑")
  class SourceMapping {

    @ParameterizedTest
    @EnumSource(value = NewsSourceUrl.class, names = {"NAVER", "HANKYUNG", "CHOSUN", "YONHAP"})
    @DisplayName("여러 소스에 대한 ArticleSource 매핑 검증")
    void map_rss_sources(NewsSourceUrl sourceUrl) {
      String xml = rssXml(
          item("기사 제목", "https://example.com/1", "Mon, 21 Apr 2026 09:00:00 GMT", "기사 요약", null));

      List<Article> result = xmlParser.parse(xml, sourceUrl);

      ArticleSource expectedSource = ArticleSource.valueOf(sourceUrl.name());
      assertEquals(expectedSource, result.get(0).getSource());
    }
  }
}
