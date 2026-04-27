package com.codeit.monew.infra.external.rss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.global.exception.external.ExternalInvalidXmlException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class XmlParserTest {

  private final XmlParser xmlParser = new XmlParser();

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
    @DisplayName("null/blank 입력이면 ExternalInvalidXmlException(empty_or_blank_xml)")
    void throw_when_invalid_input(String xml) {
      // given: null, 빈 문자열, 혹은 공백으로만 이루어진 잘못된 입력값이 주어짐

      // when: 파싱을 시도하고 예외를 캡처
      ExternalInvalidXmlException ex = assertThrows(ExternalInvalidXmlException.class,
          () -> xmlParser.parse(xml, NewsSourceUrl.CHOSUN));

      // then: ExternalInvalidXmlException이 발생하며, 상세 에러 이유가 empty_or_blank_xml임을 검증
      assertEquals("empty_or_blank_xml", ex.getDetails().get("reason"));
    }

    @Test
    @DisplayName("XML 포맷이 잘못되면 ExternalInvalidXmlException(feed_parse_failed)")
    void throw_when_feed_parse_failed() {
      // given: XML 형식이 아닌 잘못된 문자열이 주어짐

      // when: 파싱을 시도하고 예외를 캡처
      ExternalInvalidXmlException ex = assertThrows(ExternalInvalidXmlException.class,
          () -> xmlParser.parse("<not-xml>", NewsSourceUrl.CHOSUN));

      // then: ExternalInvalidXmlException이 발생하며, 상세 에러 이유가 feed_parse_failed임을 검증
      assertEquals("feed_parse_failed", ex.getDetails().get("reason"));
    }

    @Test
    @DisplayName("정상 RSS 파싱")
    void success_parse_rss() {
      // given: 필수 태그들이 모두 포함된 정상적인 구조의 RSS XML 문자열 생성
      String xml = rssXml(item(
          "기사 제목",
          "https://example.com/a",
          "Mon, 21 Apr 2026 09:00:00 GMT",
          "<b>요약</b>",
          null
      ));

      // when: RSS XML 파싱 수행
      List<Article> result = xmlParser.parse(xml, NewsSourceUrl.CHOSUN);

      // then: 파싱 결과가 1건이며, 제목/URL/요약(HTML 태그가 제거된) 데이터가 정확히 매핑되었는지 검증
      assertEquals(1, result.size());
      assertEquals("기사 제목", result.get(0).getTitle());
      assertEquals("https://example.com/a", result.get(0).getSourceUrl());
      assertEquals("요약", result.get(0).getSummary());
    }

    @Test
    @DisplayName("NAVER originallink 우선 사용")
    void use_originallink_for_naver() {
      // given: 네이버 뉴스 소스에 originallink 태그가 포함된 XML 구조 생성
      String xml = rssXml(item(
          "네이버 기사",
          "https://naver.com/link",
          "Mon, 21 Apr 2026 09:00:00 GMT",
          "요약",
          "<n:originallink xmlns:n=\"urn:naver\">https://origin.com/real</n:originallink>"
      ));

      // when: NAVER 소스로 파싱 수행
      List<Article> result = xmlParser.parse(xml, NewsSourceUrl.NAVER);

      // then: 기본 link가 아닌 originallink의 URL을 원본 주소로 추출하는지 검증
      assertEquals(1, result.size());
      assertEquals("https://origin.com/real", result.get(0).getSourceUrl());
    }

    @Test
    @DisplayName("NAVER originallink 없으면 entry.link 사용")
    void fallback_to_entry_link_for_naver() {
      // given: 네이버 뉴스 소스지만 originallink 태그가 없는 XML 구조 생성
      String xml = rssXml(item(
          "네이버 기사",
          "https://naver.com/link",
          "Mon, 21 Apr 2026 09:00:00 GMT",
          "요약",
          null
      ));

      // when: NAVER 소스로 파싱 수행
      List<Article> result = xmlParser.parse(xml, NewsSourceUrl.NAVER);

      // then: originallink가 없으므로 기본 link를 추출하는지 검증
      assertEquals("https://naver.com/link", result.get(0).getSourceUrl());
    }

    @Test
    @DisplayName("RSS는 entry.link 사용")
    void use_entry_link_for_non_naver() {
      // given: 네이버가 아닌 일반 언론사 RSS에 잘못된 originallink가 포함된 XML 생성
      String xml = rssXml(item(
          "조선 기사",
          "https://chosun.com/link",
          "Mon, 21 Apr 2026 09:00:00 GMT",
          "요약",
          "<originallink>https://ignored.com</originallink>"
      ));

      // when: CHOSUN 소스로 파싱 수행
      List<Article> result = xmlParser.parse(xml, NewsSourceUrl.CHOSUN);

      // then: 일반 RSS 파싱 로직에 따라 originallink를 무시하고 기본 link를 추출하는지 검증
      assertEquals("https://chosun.com/link", result.get(0).getSourceUrl());
    }

    @Test
    @DisplayName("description 우선으로 summary 추출")
    void summary_from_description_first() {
      // given: description과 content:encoded 태그가 모두 존재하는 XML 생성
      String xml = rssXml(item(
          "기사",
          "https://example.com/1",
          "Mon, 21 Apr 2026 09:00:00 GMT",
          "<p>description 텍스트</p>",
          "<content:encoded xmlns:content=\"http://purl.org/rss/1.0/modules/content/\">content 텍스트</content:encoded>"
      ));

      // when: 파싱 수행
      List<Article> result = xmlParser.parse(xml, NewsSourceUrl.CHOSUN);

      // then: content:encoded 대신 description 태그의 내용을 우선적으로 추출하는지 검증
      assertEquals("description 텍스트", result.get(0).getSummary());
    }

    @Test
    @DisplayName("description 없고 contents 있으면 contents로 summary 추출")
    void summary_from_contents_when_description_missing() {
      // given: description이 없고 content:encoded 태그만 존재하는 XML 생성 (CDATA 포함)
      String xml = rssXml(item(
          "기사",
          "https://example.com/2",
          "Mon, 21 Apr 2026 09:00:00 GMT",
          null,
          "<content:encoded xmlns:content=\"http://purl.org/rss/1.0/modules/content/\"><![CDATA[<div>content 본문</div>]]></content:encoded>"
      ));

      // when: 파싱 수행
      List<Article> result = xmlParser.parse(xml, NewsSourceUrl.CHOSUN);

      // then: description 대체재로 content:encoded 본문 내용이 정상 추출되는지 검증
      assertEquals("content 본문", result.get(0).getSummary());
    }

    @Test
    @DisplayName("description과 contents 모두 없으면 기본 요약 문구")
    void summary_fallback_default_message() {
      // given: description과 content:encoded 태그가 모두 누락된 XML 생성
      String xml = rssXml(item(
          "기사",
          "https://example.com/3",
          "Mon, 21 Apr 2026 09:00:00 GMT",
          null,
          null
      ));

      // when: 파싱 수행
      List<Article> result = xmlParser.parse(xml, NewsSourceUrl.CHOSUN);

      // then: 요약 내용이 빈 문자열이 아니며, 사전 정의된 기본 요약 문구로 대체되는지 검증
      assertFalse(result.get(0).getSummary().isBlank());
    }

    @Test
    @DisplayName("유효하지 않은 엔트리는 skip하고 나머지만 파싱")
    void skip_invalid_entry_and_parse_others() {
      // given: 정상 기사 1건과 필수 항목(title)이 누락된 비정상 기사 1건이 포함된 XML 생성
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

      // when: 해당 XML을 파싱 수행
      List<Article> result = xmlParser.parse(rssXml(valid, invalidNoTitle), NewsSourceUrl.CHOSUN);

      // then: 비정상 기사는 무시되고, 정상 기사 1건만 정상적으로 리스트에 담기는지 검증
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
    void map_RSS_sources(NewsSourceUrl sourceUrl) {
      // given: 단일 항목의 기본 RSS XML을 생성하고, 검증할 언론사 Enum 주입
      String xml = rssXml(
          item("기사 제목", "https://example.com/1", "Mon, 21 Apr 2026 09:00:00 GMT", "기사 요약", null));

      // when: 파라미터로 받은 언론사 소스를 기반으로 파싱 수행
      List<Article> result = xmlParser.parse(xml, sourceUrl);

      // then: 입력된 NewsSourceUrl 이름과 도메인의 ArticleSource 이름이 올바르게 매핑되는지 검증
      ArticleSource expectedSource = ArticleSource.valueOf(sourceUrl.name());
      assertEquals(expectedSource, result.get(0).getSource());
    }
  }
}