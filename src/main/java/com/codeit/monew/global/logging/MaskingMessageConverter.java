package com.codeit.monew.global.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 로그 메시지 내의 민감 정보를 마스킹 처리하는 Converter입니다. Logback 설정(xml)에서 %maskedMsg 커스텀 키워드로 사용됩니다.
 */
public class MaskingMessageConverter extends ClassicConverter {

  // 마스킹 처리할 규칙 리스트
  private static final List<MaskingRule> MASKING_RULES = List.of(
      // 1. JSON 형태의 민감 정보 마스킹 (예: "password": "1234" -> "password": "****")
      // 키 값에 password, secret, token 등이 포함되고 ':' 또는 '=' 뒤에 따옴표로 감싸진 값을 찾습니다.
      new MaskingRule(
          Pattern.compile(
              "(?i)(\"?(?:password|passwd|pwd|secret|token|authorization)\"?\\s*[:=]\\s*\")([^\"]+)(\")"),
          "$1****$3"
      ),
      // 2. Query String 또는 Key-Value 형태 마스킹 (예: password=1234 -> password=****)
      // 공백이나 '&' 기호 전까지의 연속된 문자열을 마스킹합니다.
      new MaskingRule(
          Pattern.compile("(?i)((?:password|passwd|pwd|secret|token|authorization)=)([^&\\s]+)"),
          "$1****"
      ),
      // 3. HTTP Authorization 헤더의 Bearer 토큰 마스킹
      // Bearer 뒤에 오는 JWT 토큰 등을 가립니다.
      new MaskingRule(
          Pattern.compile("(?i)(Bearer\\s+)([A-Za-z0-9\\-._~+/]+=*)"),
          "$1****"
      ),
      // 4. 이메일 주소 부분 마스킹 (예: test1234@gmail.com -> t***@gmail.com)
      // 첫 글자만 남기고 골뱅이(@) 앞부분을 모두 마스킹 처리합니다.
      new MaskingRule(
          Pattern.compile("([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*(@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})"),
          "$1***$2"
      ),
      // 5. 전화번호 마스킹 (예: 010-1234-5678 -> 010-****-5678)
      // 가운데 3~4자리를 마스킹하며, 하이픈(-)이나 공백 유무에 상관없이 동작합니다.
      new MaskingRule(
          Pattern.compile("\\b(01[0-9]|\\d{2,3})[- ]?(\\d{3,4})[- ]?(\\d{4})\\b"),
          "$1-****-$3"
      )
  );

  @Override
  public String convert(ILoggingEvent event) {
    // 로그 메시지 원본을 가져옵니다.
    String message = event.getFormattedMessage();

    if (message == null || message.isBlank()) {
      return message;
    }

    // 설정된 모든 마스킹 규칙을 순회하며 메시지를 치환합니다.
    String masked = message;
    for (MaskingRule rule : MASKING_RULES) {
      masked = rule.pattern().matcher(masked).replaceAll(rule.replacement());
    }

    return masked;
  }

  /**
   * 마스킹 규칙을 정의하는 내부 레코드
   *
   * @param pattern     탐지할 정규표현식 패턴
   * @param replacement 치환될 문자열 (캡처 그룹 활용 가능)
   */
  private record MaskingRule(Pattern pattern, String replacement) {

  }
}