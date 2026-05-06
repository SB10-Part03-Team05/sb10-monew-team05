package com.codeit.monew.global.config;

import com.codeit.monew.domain.article.dto.request.ArticleSearchRequest;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    cacheManager.setCaffeine(Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(Duration.ofMinutes(10))
    );
    cacheManager.setCacheNames(List.of("userActivity", "articleList"));
    return cacheManager;
  }

  @Bean
  public KeyGenerator articleListKeyGenerator() {
    return (target, method, params) -> {
      ArticleSearchRequest req = (ArticleSearchRequest) params[0];
      UUID userId = (UUID) params[1];
      String sortedSources = req.getSourceIn() == null ? "" :
          req.getSourceIn().stream()
              .map(Object::toString)
              .sorted()
              .collect(Collectors.joining(","));
      return String.join("_",
          req.getOrderBy().toString(),
          req.getDirection().toString(),
          Objects.toString(req.getCursor(), ""),
          Objects.toString(req.getAfter(), ""),
          Objects.toString(req.getKeyword(), ""),
          sortedSources,
          Objects.toString(req.getPublishDateFrom(), ""),
          Objects.toString(req.getPublishDateTo(), ""),
          String.valueOf(req.getLimit()),
          Objects.toString(req.getInterestId(), ""),
          userId.toString()
      );
    };
  }
}
