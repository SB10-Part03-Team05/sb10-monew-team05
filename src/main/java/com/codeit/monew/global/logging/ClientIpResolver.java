package com.codeit.monew.global.logging;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 클라이언트의 실제 IP 주소를 식별하는 컴포넌트입니다. 프록시 서버, 로드 밸런서 등을 거쳐올 경우를 대비해 다양한 HTTP 헤더를 확인합니다.
 */
@Component
public class ClientIpResolver {

  // 클라이언트 IP가 담길 수 있는 표준 및 비표준 HTTP 헤더 목록
  private static final List<String> IP_HEADERS = List.of(
      "X-Forwarded-For",    // 프록시 환경에서 가장 일반적으로 사용되는 헤더
      "X-Real-IP",         // 엔진엑스(Nginx) 등에서 실제 IP를 전달할 때 사용
      "Proxy-Client-IP",    // Apache 프록시 등에서 사용
      "WL-Proxy-Client-IP", // WebLogic 프록시에서 사용
      "HTTP_CLIENT_IP",     // 일부 프록시에서 사용
      "HTTP_X_FORWARDED_FOR"
  );

  /**
   * HttpServletRequest에서 클라이언트의 IP를 추출합니다.
   */
  public String resolve(HttpServletRequest request) {
    // 1. 정의된 헤더들을 순회하며 IP 값이 존재하는지 확인
    for (String header : IP_HEADERS) {
      String value = request.getHeader(header);
      String ip = extractClientIp(value);
      if (hasValue(ip)) {
        return ip; // 유효한 IP를 찾으면 즉시 반환
      }
    }

    // 2. 헤더에 값이 없으면 요청자의 기본 원격 주소(Remote Address) 사용
    String remoteAddr = request.getRemoteAddr();
    if (hasValue(remoteAddr)) {
      return remoteAddr;
    }

    // 3. 모든 시도가 실패하면 unknown 반환
    return "unknown";
  }

  /**
   * 헤더 값에서 실제 클라이언트 IP만 추출합니다. (X-Forwarded-For 처럼 여러 IP가 콤마로 연결된 경우 첫 번째 IP가 클라이언트의 IP입니다.)
   */
  private String extractClientIp(String headerValue) {
    if (!hasValue(headerValue)) {
      return null;
    }

    // 콤마(,)가 포함되어 있다면 첫 번째 항목만 추출
    int separatorIndex = headerValue.indexOf(',');
    if (separatorIndex < 0) {
      return headerValue.trim();
    }

    return headerValue.substring(0, separatorIndex).trim();
  }

  /**
   * 값이 유효한지(비어있지 않고 "unknown"이 아닌지) 체크합니다.
   */
  private boolean hasValue(String value) {
    return StringUtils.hasText(value) && !"unknown".equalsIgnoreCase(value.trim());
  }
}