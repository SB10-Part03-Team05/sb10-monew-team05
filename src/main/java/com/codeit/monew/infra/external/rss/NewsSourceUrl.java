package com.codeit.monew.infra.external.rss;

public enum NewsSourceUrl {
  HANKYUNG("https://www.hankyung.com/feed/all-news"),
  CHOSUN("https://www.chosun.com/arc/outboundfeeds/rss/?outputType=xml"),
  YONHAP("https://www.yonhapnewstv.co.kr/browse/feed/"),
  NAVER(
      "https://openapi.naver.com/v1/search/news.xml?query=%s&display=10"); // %s는 formatted로 나중에 넣음

  private final String urlTemplate;

  NewsSourceUrl(String urlTemplate) {
    this.urlTemplate = urlTemplate;
  }

  public String resolveRssUrl() {
    return urlTemplate;
  }

  public String resolveNaverUrl(String query) {
    if (query == null || query.isBlank()) {
      // todo: 커스텀 예외로 전환 필요
      throw new IllegalArgumentException("NAVER query는 비어 있을 수 없습니다.");
    }
    return urlTemplate.formatted(query);
  }

  public boolean isNaver() {
    return this == NAVER;
  }
}
