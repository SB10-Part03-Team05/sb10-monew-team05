package com.codeit.monew.infra.external.rss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleBodyCrawlerTest {

  @Mock
  private XmlClient xmlClient;

  @Spy
  private HankyungCrawler hankyungCrawler;

  @InjectMocks
  private ArticleBodyCrawler articleBodyCrawler;

  @Test
  @DisplayName("한국경제 본문 텍스트는 #articletxt 셀렉터로부터 추출된다")
  void extract_body_text_from_hankyung_selector() {
    // given: #articletxt 아이디를 가진 div 내에 본문이 포함된 HTML 시나리오 설정
    String url = "https://www.hankyung.com/article/202604010001";
    String html = """
        <html>
          <body>
            <div id="articletxt">
              First sentence.
              Second sentence.
            </div>
          </body>
        </html>
        """;
    given(xmlClient.fetchArticleHtml(NewsSourceUrl.HANKYUNG, url)).willReturn(html);

    // when: 본문 크롤링 로직 실행
    String body = articleBodyCrawler.crawlBodyText(url, NewsSourceUrl.HANKYUNG);

    // then: 줄바꿈이 제거되고 본문 문장들이 정상적으로 연결되었는지 검증
    assertEquals("First sentence. Second sentence.", body);
  }

  @Test
  @DisplayName("한국경제 본문은 data-index가 없는 br 태그 기반 문단을 처리한다")
  void extract_paragraphs_from_br_tags_without_data_index() {
    // given: br 태그로 구분된 여러 문단이 포함된 HTML 시나리오 설정
    String url = "https://www.hankyung.com/article/202604010002";
    String html = """
        <html>
          <body>
            <div id="articletxt" itemprop="articleBody">
              First paragraph.<br /><br />
              Second paragraph.<br class="paragraph" />
              Third paragraph.
            </div>
          </body>
        </html>
        """;
    given(xmlClient.fetchArticleHtml(NewsSourceUrl.HANKYUNG, url)).willReturn(html);

    // when: 본문 크롤링 로직 실행
    String body = articleBodyCrawler.crawlBodyText(url, NewsSourceUrl.HANKYUNG);

    // then: 각 문단이 공백으로 구분되어 하나의 문자열로 합쳐졌는지 검증
    assertEquals("First paragraph. Second paragraph. Third paragraph.", body);
  }

  @Test
  @DisplayName("한국경제 기사 본문에서 이미지 캡션 및 피규어 노이즈는 제외된다")
  void exclude_figure_and_caption_noise() {
    // given: 본문 내에 figure 및 figcaption 태그(노이즈)가 섞여 있는 시나리오 설정
    String url = "https://www.hankyung.com/article/202604010003";
    String html = """
        <html>
          <body>
            <div id="articletxt">
              <figure class="article-figure">
                <figcaption>IMAGE CAPTION</figcaption>
              </figure>
              Real body text starts here.<br />More body text.
            </div>
          </body>
        </html>
        """;
    given(xmlClient.fetchArticleHtml(NewsSourceUrl.HANKYUNG, url)).willReturn(html);

    // when: 본문 크롤링 로직 실행
    String body = articleBodyCrawler.crawlBodyText(url, NewsSourceUrl.HANKYUNG);

    // then: 캡션 내용은 제외되고 실제 기사 본문만 추출되었는지 검증
    assertEquals("Real body text starts here. More body text.", body);
  }

  @Test
  @DisplayName("추출된 본문 텍스트가 2000자를 넘으면 절삭한다")
  void return_truncated_body_text_when_exceeds_limit() {
    // given: 최대 허용 길이(2000자)를 초과하는 매우 긴 본문 텍스트 시나리오 설정
    String url = "https://www.hankyung.com/article/202604010004";
    String longText = "a".repeat(2600);
    String html = """
        <html>
          <body>
            <div id="articletxt">%s</div>
          </body>
        </html>
        """.formatted(longText);
    given(xmlClient.fetchArticleHtml(NewsSourceUrl.HANKYUNG, url)).willReturn(html);

    // when: 본문 크롤링 로직 실행
    String body = articleBodyCrawler.crawlBodyText(url, NewsSourceUrl.HANKYUNG);

    // then: 반환된 텍스트가 최대 2000자로 절삭되었는지 검증
    assertEquals(2000, body.length());
    assertTrue(body.chars().allMatch(ch -> ch == 'a'));
  }

  @Test
  @DisplayName("크롤링 실패 시 빈 문자열을 반환한다")
  void return_blank_when_crawl_failed() {
    // given: 네트워크 오류 등 런타임 예외가 발생하는 상황 모킹
    String url = "https://www.hankyung.com/article/202604010005";
    given(xmlClient.fetchArticleHtml(NewsSourceUrl.HANKYUNG, url))
        .willThrow(new RuntimeException("network error"));

    // when: 본문 크롤링 로직 실행
    String body = articleBodyCrawler.crawlBodyText(url, NewsSourceUrl.HANKYUNG);

    // then: 예외가 전파되지 않고 안전하게 빈 문자열("")이 반환되는지 검증
    assertEquals("", body);
  }

  @Test
  @DisplayName("같은 URL/소스 재호출 시 캐시를 사용해 외부 HTML 요청을 생략한다")
  void use_cache_on_second_call() {
    // given: 동일한 URL에 대한 두 번의 호출 시나리오 설정
    String url = "https://www.hankyung.com/article/202604010006";
    String html = """
        <html>
          <body>
            <div id="articletxt">Cached body text.</div>
          </body>
        </html>
        """;
    given(xmlClient.fetchArticleHtml(NewsSourceUrl.HANKYUNG, url)).willReturn(html);

    // when: 첫 번째 및 두 번째 호출 수행
    String first = articleBodyCrawler.crawlBodyText(url, NewsSourceUrl.HANKYUNG);
    String second = articleBodyCrawler.crawlBodyText(url, NewsSourceUrl.HANKYUNG);

    // then: 두 결과가 동일하며, 실제 외부 네트워크 요청(xmlClient)은 단 1회만 발생했는지 검증
    assertEquals("Cached body text.", first);
    assertEquals("Cached body text.", second);
    verify(xmlClient, times(1)).fetchArticleHtml(NewsSourceUrl.HANKYUNG, url);
  }

  @Test
  @DisplayName("URL이 null/blank면 즉시 빈 문자열을 반환하고 외부 요청을 하지 않는다")
  void return_blank_when_url_is_null_or_blank() {
    // when & then: URL이 비어있을 때 즉시 빈 문자열이 반환되는지 검증
    assertEquals("", articleBodyCrawler.crawlBodyText(null, NewsSourceUrl.HANKYUNG));
    assertEquals("", articleBodyCrawler.crawlBodyText("   ", NewsSourceUrl.HANKYUNG));

    // 외부 클라이언트나 자식 크롤러와 아무런 상호작용이 없어야 함
    verifyNoInteractions(xmlClient);
    verifyNoInteractions(hankyungCrawler);
  }

  @Test
  @DisplayName("지원하지 않는 소스(HANKYUNG 외)는 네트워크 요청 없이 즉시 빈 문자열을 반환한다")
  void return_blank_for_non_hankyung_source() {
    // given: 크롤러 맵에 등록되지 않은 소스(조선일보) 준비
    String url = "https://www.chosun.com/article/202604010007";

    // when: 크롤링 시도
    String body = articleBodyCrawler.crawlBodyText(url, NewsSourceUrl.CHOSUN);

    // then: 결과는 빈 문자열이어야 하며 xmlClient 호출 발생하지 않아야 함 (Fail-Fast)
    assertEquals("", body);
    verifyNoInteractions(xmlClient);
    verify(hankyungCrawler, never()).crawl(anyString());
  }

  @Test
  @DisplayName("빈 본문 결과는 캐시하지 않아 재호출 시 외부 요청을 다시 수행한다")
  void blank_result_is_not_cached() {
    // given: 본문 추출 결과가 비어있는 상황 설정 (캐시 저장 방지 조건 테스트)
    String url = "https://www.chosun.com/article/202604010008";
    given(xmlClient.fetchArticleHtml(NewsSourceUrl.HANKYUNG, url))
        .willReturn("<html><body>x</body></html>");

    // when: 동일 URL로 두 번 호출
    articleBodyCrawler.crawlBodyText(url, NewsSourceUrl.HANKYUNG);
    articleBodyCrawler.crawlBodyText(url, NewsSourceUrl.HANKYUNG);

    // then: 결과가 비어있었으므로 캐시되지 않고 매번 외부 요청(xmlClient)을 다시 수행하는지 검증
    verify(xmlClient, times(2)).fetchArticleHtml(NewsSourceUrl.HANKYUNG, url);
  }
}