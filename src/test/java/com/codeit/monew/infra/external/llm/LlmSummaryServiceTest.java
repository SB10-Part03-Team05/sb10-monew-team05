package com.codeit.monew.infra.external.llm;

import static com.codeit.monew.global.common.constant.ArticleSummaryConstants.DEFAULT_SUMMARY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.monew.infra.external.llm.gemini.GeminiLlmSummarizer;
import com.codeit.monew.infra.external.llm.openai.OpenAiLlmSummarizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class LlmSummaryServiceTest {

  @Mock
  private ObjectProvider<OpenAiLlmSummarizer> openAiProvider;
  @Mock
  private ObjectProvider<GeminiLlmSummarizer> geminiProvider;
  @Mock
  private OpenAiLlmSummarizer openAiSummarizer;
  @Mock
  private GeminiLlmSummarizer geminiSummarizer;

  @Test
  @DisplayName("본문이 null/blank면 요약 시도 없이 기본 문구를 반환한다")
  void return_original_when_body_text_is_blank() {
    // given: 서비스 초기화 및 빈 값 테스트 데이터 준비
    LlmSummaryService service = new LlmSummaryService(openAiProvider, geminiProvider);

    // when: 본문이 null이거나 공백인 상태로 요약 요청
    String resultNull = service.summarizeOrOriginal(null, "https://a.com");
    String resultBlank = service.summarizeOrOriginal(" ", "https://a.com");

    // then: 어떠한 LLM 프로바이더도 조회하지 않고 기본 문구가 반환되는지 검증
    assertEquals(DEFAULT_SUMMARY, resultNull);
    assertEquals(DEFAULT_SUMMARY, resultBlank);
    verify(openAiProvider, never()).getIfAvailable();
    verify(geminiProvider, never()).getIfAvailable();
  }

  @Test
  @DisplayName("본문이 기본 문구면 요약 시도 없이 기본 문구를 반환한다")
  void return_default_summary_when_body_text_is_default_summary() {
    // given
    LlmSummaryService service = new LlmSummaryService(openAiProvider, geminiProvider);

    // when
    String result = service.summarizeOrOriginal(DEFAULT_SUMMARY, "https://a.com");

    // then
    assertEquals(DEFAULT_SUMMARY, result);
    verify(openAiProvider, never()).getIfAvailable();
    verify(geminiProvider, never()).getIfAvailable();
  }

  @Test
  @DisplayName("OpenAI 요약 성공 시 결과를 trim해서 반환하고 Gemini는 호출하지 않는다")
  void return_openai_summary_when_openai_succeeds() {
    // given: OpenAI 프로바이더가 가용하며 정상적인 요약본을 반환하도록 설정
    LlmSummaryService service = new LlmSummaryService(openAiProvider, geminiProvider);
    when(openAiProvider.getIfAvailable()).thenReturn(openAiSummarizer);
    when(openAiSummarizer.summarize("body text")).thenReturn("  openai summary  ");

    // when: 요약 요청 수행
    String result = service.summarizeOrOriginal("body text", "https://a.com/1");

    // then: 양 끝 공백이 제거된 요약본이 반환되고, 2순위인 Gemini는 조회조차 하지 않았는지 검증
    assertEquals("openai summary", result);
    verify(geminiProvider, never()).getIfAvailable();
  }

  @Test
  @DisplayName("OpenAI 실패 시 Gemini fallback 결과를 반환한다")
  void return_gemini_summary_when_openai_fails() {
    // given: OpenAI 호출 시 예외가 발생하고, Gemini는 정상 동작하는 Fallback 상황 설정
    LlmSummaryService service = new LlmSummaryService(openAiProvider, geminiProvider);
    when(openAiProvider.getIfAvailable()).thenReturn(openAiSummarizer);
    when(geminiProvider.getIfAvailable()).thenReturn(geminiSummarizer);
    when(openAiSummarizer.summarize("body text")).thenThrow(new RuntimeException("openai down"));
    when(geminiSummarizer.summarize("body text")).thenReturn("gemini summary");

    // when: 요약 요청 수행
    String result = service.summarizeOrOriginal("body text", "https://a.com/2");

    // then: 1순위 실패를 딛고 2순위인 Gemini 요약 결과가 정상 반환되었는지 검증
    assertEquals("gemini summary", result);
  }

  @Test
  @DisplayName("OpenAI가 blank 응답이면 Gemini fallback을 시도한다")
  void fallback_to_gemini_when_openai_returns_blank() {
    // given: OpenAI가 에러는 아니지만 실제 요약 내용이 없는(공백) 응답을 주는 상황 설정
    LlmSummaryService service = new LlmSummaryService(openAiProvider, geminiProvider);
    when(openAiProvider.getIfAvailable()).thenReturn(openAiSummarizer);
    when(geminiProvider.getIfAvailable()).thenReturn(geminiSummarizer);
    when(openAiSummarizer.summarize("body text")).thenReturn("   ");
    when(geminiSummarizer.summarize("body text")).thenReturn("gemini summary");

    // when: 요약 요청 수행
    String result = service.summarizeOrOriginal("body text", "https://a.com/3");

    // then: 공백 응답을 실패로 간주하고 Gemini 요약 결과를 성공적으로 가져왔는지 검증
    assertEquals("gemini summary", result);
  }

  @Test
  @DisplayName("두 provider가 모두 실패하면 원문을 반환한다")
  void return_original_when_all_providers_fail_or_unavailable() {
    // given: 1순위는 빈(Bean)이 없고, 2순위는 호출 중 예외가 발생하는 '전체 실패' 상황 설정
    LlmSummaryService service = new LlmSummaryService(openAiProvider, geminiProvider);
    when(openAiProvider.getIfAvailable()).thenReturn(null);
    when(geminiProvider.getIfAvailable()).thenReturn(geminiSummarizer);
    when(geminiSummarizer.summarize("body text")).thenThrow(new RuntimeException("gemini down"));

    // when: 요약 요청 수행
    String result = service.summarizeOrOriginal("body text", "https://a.com/4");

    // then: 서비스 중단 없이 안전하게 원문(Original Text)이 반환되었는지 검증
    assertEquals("body text", result);
  }
}
