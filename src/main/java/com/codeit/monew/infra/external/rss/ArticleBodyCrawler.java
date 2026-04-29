package com.codeit.monew.infra.external.rss;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * RSS 피드 내 기사 링크를 방문하여 본문 텍스트를 추출하고, 반복적인 네트워크 요청을 줄이기 위해 인메모리 캐싱을 수행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleBodyCrawler {

  private static final Duration CACHE_TTL = Duration.ofHours(6);
  private static final int MAX_CACHE_SIZE = 200;
  private static final int MAX_CRAWLED_BODY_LENGTH = 2000;
  private static final long CRAWL_COOLDOWN_MILLIS = 1000L;

  // 인메모리 캐시 저장소 (Thread-safe)
  private final Map<CacheKey, CacheValue> bodyTextCache = new ConcurrentHashMap<>();

  private final XmlClient xmlClient;
  private final HankyungCrawler hankyungCrawler;
  // private final MaeilCrawler maeilCrawler; // 추후 추가 예정

  /**
   * 특정 매체의 기사 URL로부터 본문 텍스트를 크롤링합니다.
   */
  public String crawlBodyText(String articleUrl, NewsSourceUrl source) {
    if (!StringUtils.hasText(articleUrl)) {
      return "";
    }

    String normalizedUrl = articleUrl.trim();

    // 1. 캐시 확인 (Cache-Aside 패턴)
    CacheLookupResult cacheLookupResult = findCachedBodyText(source, normalizedUrl);
    if (cacheLookupResult.hit()) {
      log.debug("[{}] crawl cache hit. url={}", source, normalizedUrl);
      return limitBodyLength(cacheLookupResult.bodyText());
    }

    log.debug("[{}] crawl cache miss. starting extraction. url={}", source, normalizedUrl);

    // 2. 크롤링 수행
    try {
      applyCrawlCooldown();
      // 외부 서버에서 HTML 원문 fetch
      String html = xmlClient.fetchArticleHtml(source, normalizedUrl);

      // 매체별 전용 크롤러로 본문 텍스트 추출 (Strategy 패턴 방식)
      String crawledBodyText = switch (source) {
        case HANKYUNG -> hankyungCrawler.crawl(html);
        // case MAEIL -> maeilCrawler.crawl(html);
        default -> "";
      };
      
      // 기사 원문이 너무 길 경우 자르기
      String normalizedBodyText = limitBodyLength(crawledBodyText);

      // 3. 크롤링 성공 시 캐시 저장
      if (StringUtils.hasText(normalizedBodyText)) {
        cacheBodyText(source, normalizedUrl, normalizedBodyText);
        log.debug("[{}] crawl cache store. url={}", source, normalizedUrl);
      }

      return normalizedBodyText;

    } catch (RuntimeException e) {
      // 크롤러 장애가 전체 배치 프로세스(XmlParser)에 영향을 주지 않도록 방어
      log.warn("[{}] article crawl failed(fallback to empty). url={}, error={}, message={}",
          source, normalizedUrl, e.getClass().getSimpleName(), e.getMessage());
      return "";
    }
  }

  /**
   * 캐시에서 본문이 존재하는지 확인하고, 만료된 경우 삭제합니다.
   */
  private CacheLookupResult findCachedBodyText(NewsSourceUrl source, String articleUrl) {
    CacheKey key = new CacheKey(source, articleUrl);
    CacheValue value = bodyTextCache.get(key);

    if (value == null) {
      return new CacheLookupResult(false, "");
    }

    // 만료 시간 검증
    if (value.expiresAt().isBefore(Instant.now())) {
      bodyTextCache.remove(key, value);
      log.debug("[{}] crawl cache expired. url={}", source, articleUrl);
      return new CacheLookupResult(false, "");
    }

    return new CacheLookupResult(true, value.bodyText());
  }

  /**
   * 크롤링된 본문을 캐시에 저장합니다. 저장 전 공간 확보를 위해 만료된 항목을 정리합니다.
   */
  private void cacheBodyText(NewsSourceUrl source, String articleUrl, String bodyText) {
    evictExpiredEntriesIfNeeded();

    if (bodyTextCache.size() >= MAX_CACHE_SIZE) {
      log.warn("Cache size reached limit({}). Skipping store.", MAX_CACHE_SIZE);
      return;
    }

    bodyTextCache.put(
        new CacheKey(source, articleUrl),
        new CacheValue(bodyText, Instant.now().plus(CACHE_TTL))
    );
  }

  /**
   * 캐시 공간이 부족할 경우 만료된 데이터를 일괄 삭제합니다.
   */
  private void evictExpiredEntriesIfNeeded() {
    if (bodyTextCache.size() < MAX_CACHE_SIZE) {
      return;
    }

    Instant now = Instant.now();
    bodyTextCache.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
  }

  /**
   * 크롤링 시 봇 차단을 방지하기 위해 쿨다은을 적용합니다.
   */
  private void applyCrawlCooldown() {
    try {
      Thread.sleep(CRAWL_COOLDOWN_MILLIS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("crawl cooldown interrupted");
    }
  }

  /**
   * 기사 원문이 너무 길 경우 자릅니다.
   */
  private String limitBodyLength(String bodyText) {
    if (!StringUtils.hasText(bodyText) || bodyText.length() <= MAX_CRAWLED_BODY_LENGTH) {
      return bodyText;
    }
    return bodyText.substring(0, MAX_CRAWLED_BODY_LENGTH);
  }

  // --- 내부 데이터 구조 (Value Objects) ---

  private record CacheKey(NewsSourceUrl source, String articleUrl) {

  }

  private record CacheValue(String bodyText, Instant expiresAt) {

  }

  private record CacheLookupResult(boolean hit, String bodyText) {

  }
}
