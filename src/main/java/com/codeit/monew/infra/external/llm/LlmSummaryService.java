package com.codeit.monew.infra.external.llm;

import com.codeit.monew.global.exception.external.llm.ExternalLlmException;
import com.codeit.monew.global.exception.external.llm.ExternalLlmProviderException;
import com.codeit.monew.infra.external.llm.gemini.GeminiLlmSummarizer;
import com.codeit.monew.infra.external.llm.openai.OpenAiLlmSummarizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 외부 LLM(Large Language Model) API를 사용하여 뉴스 기사 본문을 요약하는 서비스입니다.
 * <p>
 * 현재 OpenAI와 Gemini 두 가지 프로바이더를 지원하며, 하나가 실패하거나 사용 불가능할 경우 다음 프로바이더를 시도하는 Fallback 전략을 수행합니다. 모든 요약
 * 시도가 실패할 경우 원문(Original Text)을 반환하여 시스템의 안정성을 유지합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmSummaryService {

  private static final String DEFAULT_SUMMARY = "요약이 제공되지 않는 출처입니다";

  /**
   * 의존성 주입 시 빈(Bean)이 존재하지 않아도 애플리케이션 컨텍스트 로딩이 실패하지 않도록 {@link ObjectProvider}를 사용하여 런타임에 빈을
   * 조회합니다.
   */
  private final ObjectProvider<OpenAiLlmSummarizer> openAiLlmSummarizerProvider;
  private final ObjectProvider<GeminiLlmSummarizer> geminiLlmSummarizerProvider;

  /**
   * 입력받은 본문 텍스트를 요약합니다. OpenAI -> Gemini 순으로 시도하며, 요약 실패 시 원문을 그대로 반환합니다.
   *
   * @param bodyText  요약할 기사 본문 원문
   * @param sourceUrl 로그 기록 및 추적을 위한 기사 원문 링크
   * @return 요약된 텍스트. 모든 시도 실패 시 또는 본문이 비어있을 시 {@code bodyText} 그대로 반환
   */
  public String summarizeOrOriginal(String bodyText, String sourceUrl) {
    // 본문이 없으면 요약할 대상이 없으므로 즉시 반환
    if (!StringUtils.hasText(bodyText)) {
      return DEFAULT_SUMMARY;
    }

    // 1순위: OpenAI를 통한 요약 시도
    String openAiSummary = trySummarize(
        "openai",
        openAiLlmSummarizerProvider.getIfAvailable(),
        bodyText,
        sourceUrl
    );
    if (StringUtils.hasText(openAiSummary)) {
      return openAiSummary;
    }

    // 2순위: OpenAI 실패 시 Gemini를 통한 요약 시도 (Fallback)
    String geminiSummary = trySummarize(
        "gemini",
        geminiLlmSummarizerProvider.getIfAvailable(),
        bodyText,
        sourceUrl
    );
    if (StringUtils.hasText(geminiSummary)) {
      return geminiSummary;
    }

    // 모든 LLM 요약이 실패한 경우, 원문이라도 제공하기 위해 원본 반환
    log.warn("[LLM] All providers failed or returned blank. returning original text. url={}",
        sourceUrl);
    return bodyText;
  }

  /**
   * 특정 LLM 프로바이더를 사용하여 요약을 시도합니다. 예외 상황(네트워크 에러, API 키 오류 등) 발생 시 에러를 전파하지 않고 로그만 남긴 뒤 빈 문자열을
   * 반환합니다.
   *
   * @param provider   로그 식별을 위한 프로바이더 명칭 (openai, gemini 등)
   * @param summarizer 실제 요약 로직을 수행할 객체 (Bean 미등록 시 null 가능)
   * @param bodyText   요약 대상 텍스트
   * @param sourceUrl  에러 발생 시 추적을 위한 URL
   * @return 요약 성공 시 요약 텍스트(trim 적용), 실패 시 빈 문자열("")
   */
  private String trySummarize(String provider, LlmSummarizer summarizer, String bodyText,
      String sourceUrl) {
    // 해당 프로바이더 빈이 컨테이너에 등록되어 있지 않은 경우
    if (summarizer == null) {
      log.warn("[LLM] provider={} unavailable (Bean not found). url={}", provider, sourceUrl);
      return "";
    }

    try {
      String summary = summarizer.summarize(bodyText);

      // 응답이 비어있는 경우 (예: 필터링에 걸리거나 모델이 빈 응답을 준 경우)
      if (!StringUtils.hasText(summary)) {
        log.warn("[LLM] provider={} returned blank summary. url={}", provider, sourceUrl);
        return "";
      }

      return summary.trim();
    } catch (ExternalLlmException e) {
      // 외부 API 호출 실패 시에도 전체 기사 수집 프로세스가 중단되지 않도록 예외를 흡수(Graceful Degradation)
      log.warn("[LLM] provider={} summary failed. url={}, errorType={}, message={}",
          provider, sourceUrl, e.getClass().getSimpleName(), e.getMessage());
      return "";
    } catch (RuntimeException e) {
      ExternalLlmProviderException wrapped = new ExternalLlmProviderException(provider, sourceUrl,
          e);
      log.warn("[LLM] provider={} summary failed. url={}, errorType={}, message={}",
          provider, sourceUrl, wrapped.getClass().getSimpleName(), wrapped.getMessage());
      return "";
    }
  }
}
