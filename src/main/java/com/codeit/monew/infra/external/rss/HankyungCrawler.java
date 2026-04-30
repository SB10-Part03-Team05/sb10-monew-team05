package com.codeit.monew.infra.external.rss;

import java.util.List;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HankyungCrawler extends CommonArticleCrawler {

  // 한경 전용 타겟 및 노이즈 세팅
  private static final String NOISE_SELECTORS = "script, style, figure, figcaption, iframe, noscript";
  private static final List<String> TARGET_SELECTORS = List.of(
      "#articletxt",
      "article #articletxt",
      "article.article-body",
      ".article-body",
      ".news-view .article-body",
      "div[itemprop=articleBody]"
  );

  @Override
  protected List<String> getTargetSelectors() {
    return TARGET_SELECTORS;
  }

  @Override
  protected String getNoiseSelectors() {
    return NOISE_SELECTORS;
  }

  @Override
  protected String extractText(Element element) {
    // 한경은 br 태그를 살려서 요약을 만들어야 가독성이 좋음
    element.select("br").forEach(br -> br.replaceWith(new TextNode("\n")));

    String bodyText = element.wholeText();
    if (StringUtils.hasText(bodyText)) {
      return bodyText;
    }
    return element.text();
  }
}