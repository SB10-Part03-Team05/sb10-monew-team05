package com.codeit.monew.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

  @Bean
  public RestClient restClient() {
    // HTTP 통신을 위한 팩토리 설정 (타임아웃 등)
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5000); // 연결 타임아웃 5초
    factory.setReadTimeout(10000);   // 읽기 타임아웃 10초

    return RestClient.builder()
        .requestFactory(factory)
        .build();
  }
}