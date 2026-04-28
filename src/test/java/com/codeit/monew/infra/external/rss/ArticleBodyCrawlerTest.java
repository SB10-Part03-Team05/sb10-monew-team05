package com.codeit.monew.infra.external.rss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleBodyCrawlerTest {

  @Mock
  private XmlClient xmlClient;

  @InjectMocks
  private ArticleBodyCrawler articleBodyCrawler;

  @Test
  @DisplayName("Hankyung body text is extracted from #articletxt")
  void extract_body_text_from_hankyung_selector() {
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

    String body = articleBodyCrawler.crawlBodyText(url, NewsSourceUrl.HANKYUNG);

    assertEquals("First sentence. Second sentence.", body);
  }

  @Test
  @DisplayName("Hankyung body uses br-delimited paragraphs without data-index")
  void extract_paragraphs_from_br_tags_without_data_index() {
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

    String body = articleBodyCrawler.crawlBodyText(url, NewsSourceUrl.HANKYUNG);

    assertEquals("First paragraph. Second paragraph. Third paragraph.", body);
  }

  @Test
  @DisplayName("Figure/caption noise is excluded from Hankyung article body")
  void exclude_figure_and_caption_noise() {
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

    String body = articleBodyCrawler.crawlBodyText(url, NewsSourceUrl.HANKYUNG);

    assertEquals("Real body text starts here. More body text.", body);
  }

  @Test
  @DisplayName("Body text is not truncated")
  void return_full_body_text_without_truncation() {
    String url = "https://www.hankyung.com/article/202604010004";
    String longText = "a".repeat(260);
    String html = """
        <html>
          <body>
            <div id="articletxt">%s</div>
          </body>
        </html>
        """.formatted(longText);
    given(xmlClient.fetchArticleHtml(NewsSourceUrl.HANKYUNG, url)).willReturn(html);

    String body = articleBodyCrawler.crawlBodyText(url, NewsSourceUrl.HANKYUNG);

    assertEquals(260, body.length());
    assertTrue(body.chars().allMatch(ch -> ch == 'a'));
  }

  @Test
  @DisplayName("Blank string is returned when crawl fails")
  void return_blank_when_crawl_failed() {
    String url = "https://www.hankyung.com/article/202604010005";
    given(xmlClient.fetchArticleHtml(NewsSourceUrl.HANKYUNG, url))
        .willThrow(new RuntimeException("network error"));

    String body = articleBodyCrawler.crawlBodyText(url, NewsSourceUrl.HANKYUNG);

    assertEquals("", body);
  }
}
