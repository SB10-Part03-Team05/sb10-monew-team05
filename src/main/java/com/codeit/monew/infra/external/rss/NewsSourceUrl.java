package com.codeit.monew.infra.external.rss;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.global.exception.common.InvalidParameterException;
import lombok.Getter;

@Getter
public enum NewsSourceUrl {
  HANKYUNG("https://www.hankyung.com/feed/all-news", ArticleSource.HANKYUNG),
  CHOSUN("https://www.chosun.com/arc/outboundfeeds/rss/?outputType=xml", ArticleSource.CHOSUN),
  YONHAP("https://www.yonhapnewstv.co.kr/browse/feed/", ArticleSource.YONHAP),
  NAVER("https://openapi.naver.com/v1/search/news.xml?query=%s&display=10",
      ArticleSource.NAVER), // %s는 formatted로 나중에 넣음
  YNA("https://www.yna.co.kr/rss/news.xml", ArticleSource.YNA),
  JTBC("https://news-ex.jtbc.co.kr/v1/get/rss/newsflesh", ArticleSource.JTBC),
  KHAN("https://www.khan.co.kr/rss/rssdata/total_news.xml", ArticleSource.KHAN),
  DONGA("https://rss.donga.com/total.xml", ArticleSource.DONGA),
  MK("https://www.mk.co.kr/rss/40300001/", ArticleSource.MK),
  SBS("https://news.sbs.co.kr/news/newsflashRssFeed.do?plink=RSSREADER", ArticleSource.SBS);

  private final String urlTemplate;
  private final ArticleSource articleSource;

  NewsSourceUrl(String urlTemplate, ArticleSource articleSource) {
    this.urlTemplate = urlTemplate;
    this.articleSource = articleSource;
  }

  public String resolveRssUrl() {
    return urlTemplate;
  }

  public String resolveNaverUrl(String query) {
    if (query == null || query.isBlank()) {
      throw new InvalidParameterException("query", query, "source", NAVER.name());
    }
    return urlTemplate.formatted(query);
  }

  public boolean isNaver() {
    return this == NAVER;
  }
}
