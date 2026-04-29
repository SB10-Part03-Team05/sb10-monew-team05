package com.codeit.monew.global.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiConnectionProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * LLM(Large Language Model) 클라이언트들의 네트워크 타임아웃 및 관련 설정을 관리하는 구성 클래스입니다.
 * <p>
 * 뉴스 요약과 같은 외부 API 호출 시, 응답 지연이 전체 시스템의 스레드 점유로 이어지지 않도록 모든 LLM 관련 요청에 공통적인 타임아웃 제약 조건을 적용합니다.
 */
@Slf4j
@Configuration
public class LlmClientTimeoutConfig {

  /**
   * LLM 서비스 응답 대기를 위한 최대 시간 (15초). 연결(Connect) 및 읽기(Read) 타임아웃에 공통으로 적용됩니다.
   */
  private static final int LLM_TIMEOUT_MILLIS = 30_000;

  /**
   * Spring {@link org.springframework.web.client.RestClient}를 사용하는 클라이언트를 위한 타임아웃 커스터마이저입니다. 주로
   * OpenAI 등 HTTP 기반의 표준 REST 호출을 수행하는 라이브러리에 적용됩니다.
   *
   * @return {@link RestClientCustomizer} 15초 타임아웃 팩토리가 적용된 설정 객체
   */
  @Bean
  public RestClientCustomizer llmRestClientCustomizer() {
    return restClientBuilder -> {
      SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
      factory.setConnectTimeout(LLM_TIMEOUT_MILLIS);
      factory.setReadTimeout(LLM_TIMEOUT_MILLIS);
      restClientBuilder.requestFactory(factory);
      log.info("[LLM-Config] RestClient 기반 타임아웃 설정 완료 ({}ms)", LLM_TIMEOUT_MILLIS);
    };
  }

  /**
   * Google GenAI SDK용 {@link Client} 빈을 생성합니다.
   * <p>
   * Gemini SDK는 내부적으로 별도의 HTTP 클라이언트를 사용하므로, 클라이언트 빌드 시점에 직접 {@link HttpOptions}를 통해 타임아웃을 주입해야
   * 합니다. 이 빈은 설정 파일에 API Key가 존재할 때만 생성됩니다.
   *
   * @param properties Spring AI Google GenAI 연결 설정 정보 (API Key 추출용)
   * @return 타임아웃과 API Key가 설정된 Gemini {@link Client} 객체
   */
  @Bean
  @ConditionalOnClass(Client.class)
  @ConditionalOnProperty(prefix = "spring.ai.google.genai", name = "api-key")
  public Client googleGenAiClient(GoogleGenAiConnectionProperties properties) {
    // Gemini SDK 전용 타임아웃 옵션 구성
    HttpOptions timeoutOptions = HttpOptions.builder()
        .timeout(LLM_TIMEOUT_MILLIS)
        .build();

    log.info("[LLM-Config] Gemini Client 생성 완료 (Timeout: {}ms)", LLM_TIMEOUT_MILLIS);

    // API Key와 타임아웃 옵션을 결합하여 클라이언트 반환
    return Client.builder()
        .apiKey(properties.getApiKey())
        .httpOptions(timeoutOptions)
        .build();
  }
}