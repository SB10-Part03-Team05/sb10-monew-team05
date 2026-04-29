package com.codeit.monew.infra.external.llm.gemini;

import com.codeit.monew.global.exception.external.llm.ExternalLlmInvalidInputException;
import com.codeit.monew.global.exception.external.llm.ExternalLlmProviderException;
import com.codeit.monew.infra.external.llm.LlmSummarizer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiLlmSummarizer implements LlmSummarizer {

  private static final String SYSTEM_PROMPT = """
      너는 한국어 뉴스 기사 요약기다.
      반드시 제공된 기사 본문에 실제로 존재하는 정보만 사용한다.
      기사 본문에 없는 키워드, 인물, 회사명, 수치, 전망, 해석을 새로 만들지 않는다.
      요약은 한글 기준 약 200자 내외로 작성한다.
      한 문단으로만 작성한다.
      """;

  private final GoogleGenAiChatModel geminiChatModel;

  @Override
  public String summarize(String bodyText) {
    if (!StringUtils.hasText(bodyText)) {
      throw new ExternalLlmInvalidInputException("gemini", "bodyText");
    }

    try {
      Prompt prompt = new Prompt(List.of(
          new SystemMessage(SYSTEM_PROMPT),
          new UserMessage(buildUserPrompt(bodyText))
      ));

      // 응답 객체 전체를 가져옵니다.
      var response = geminiChatModel.call(prompt);

      // 요약 결과 추출
      String summary = response.getResult().getOutput().getText();

      // 토큰 메타데이터 추출 및 디버그 로그 기록
      var usage = response.getMetadata().getUsage();
      log.debug("[LLM-Gemini] Summary completed. Tokens: [In: {}, Out: {}, Total: {}]\nSummary: {}",
          usage.getPromptTokens(),
          usage.getCompletionTokens(),
          usage.getTotalTokens(),
          summary);

      return summary;
    } catch (RuntimeException e) {
      throw new ExternalLlmProviderException("gemini", null, e);
    }
  }

  private String buildUserPrompt(String articleText) {
    return """
        다음 기사 본문에서 중요한 내용만 약 200자 내외로 요약해줘.
        
        주의:
        - 기사에 없는 키워드를 만들지 마.
        - 기사에 나온 핵심 문장과 표현을 기반으로 압축해.
        - 새로운 원인, 전망, 평가를 추가하지 마.
        - 결과는 요약문만 출력해.
        
        [기사 본문]
        %s
        """.formatted(articleText).trim();
  }
}
