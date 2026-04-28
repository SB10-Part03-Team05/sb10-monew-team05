package com.codeit.monew.infra.external.rss;

import com.codeit.monew.global.exception.external.client.ExternalClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleBodyCrawler {

  private final XmlClient xmlClient;
  private final HankyungCrawler hankyungCrawler;
  // 추후 매일경제 등이 추가되면 여기에 주입: private final MaeilCrawler maeilCrawler;

  public String crawlBodyText(String articleUrl, NewsSourceUrl source) {
    if (!StringUtils.hasText(articleUrl)) {
      return "";
    }

    try {
      // 1. 외부 서버에서 HTML 원문 가져오기
      String html = xmlClient.fetchArticleHtml(source, articleUrl);

      // 2. 매체별 알맞은 자식 크롤러로 라우팅
      return switch (source) {
        case HANKYUNG -> hankyungCrawler.crawl(html);
//        case MAEIL -> maeilCrawler.crawl(html); // 나중에 추가할 곳
        default -> ""; // 현재는 한경 외에는 빈 값 리턴 -> XmlParser에서 DEFAULT_SUMMARY로 처리됨
      };

    } catch (RuntimeException e) {
      // 크롤러가 죽어도 파서 전체가 죽지 않도록 방어 (XmlParser의 Graceful Degradation을 위해)
      log.warn("[{}] article crawl failed. url={}, errorType={}, message={}",
          source, articleUrl, e.getClass().getSimpleName(), e.getMessage());
      return "";
    }
  }
}