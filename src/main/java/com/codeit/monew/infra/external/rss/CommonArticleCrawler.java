package com.codeit.monew.infra.external.rss;

import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.util.StringUtils;

public abstract class CommonArticleCrawler {

  /**
   * 템플릿 메서드: 파싱 -> 추출 -> 노이즈 제거 -> 정규화의 전체 흐름
   */
  public String crawl(String html) {
    // 크롤링한 html이 빈 값이면 빈 값 반환
    if (!StringUtils.hasText(html)) {
      return "";
    }

    Document doc = Jsoup.parse(html);
    Element root = findRoot(doc);

    // 셀렉터로 본문 영역을 못 찾은 경우 빈 값 반환
    if (root == null) {
      return "";
    }

    // 본문 영역을 찾은 경우: 노이즈 제거 후 자식의 방식대로 추출
    removeNoise(root);
    String text = extractText(root);

    // 정제 후 텍스트가 존재할 때 결과 반환, 없으면 빈 값 반환
    return StringUtils.hasText(text) ? normalize(text) : "";
  }

  private Element findRoot(Document doc) {
    for (String selector : getTargetSelectors()) {
      Element found = doc.selectFirst(selector);
      if (found != null && StringUtils.hasText(found.text())) {
        return found.clone(); // 원본 보호
      }
    }
    return null;
  }

  protected void removeNoise(Element element) {
    String noiseSelectors = getNoiseSelectors();
    if (StringUtils.hasText(noiseSelectors)) {
      element.select(noiseSelectors).remove();
    }
  }

  // --- 자식들이 재정의할 수 있는 확장 포인트 ---
  protected abstract List<String> getTargetSelectors();

  protected abstract String getNoiseSelectors();

  // 기본은 단순 text() 추출, 필요시 자식에서 오버라이드 (ex. wholeText)
  protected String extractText(Element element) {
    return element.text();
  }

  // 공백 및 줄바꿈 정규화
  protected String normalize(String text) {
    if (!StringUtils.hasText(text)) {
      return "";
    }
    return text
        .replaceAll("[\\r\\n\\t]+", " ")
        .replaceAll("\\s+", " ")
        .trim();
  }
}