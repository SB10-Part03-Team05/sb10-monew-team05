package com.codeit.monew.global.config;

import com.codeit.monew.global.logging.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 요청마다 MDC에 컨텍스트 정보를 추가하는 인터셉터
 */
@Slf4j
@RequiredArgsConstructor
public class MDCLoggingInterceptor implements HandlerInterceptor {

  /**
   * MDC 로깅에 사용되는 상수 정의
   */
  public static final String REQUEST_ID = "requestId";
  public static final String CLIENT_IP = "clientIp";
  public static final String REQUEST_METHOD = "requestMethod";
  public static final String REQUEST_URI = "requestUri";

  public static final String REQUEST_ID_HEADER = "Monew-Request-ID";
  public static final String REQUEST_IP_HEADER = "Monew-Request-IP";

  private final ClientIpResolver clientIpResolver;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
      Object handler) {
    // 요청 ID 생성 (UUID)
    String requestId = UUID.randomUUID().toString().replaceAll("-", "");

    // 요청 IP 생성
    String clientIp = clientIpResolver.resolve(request);

    // MDC에 컨텍스트 정보 추가
    MDC.put(REQUEST_ID, requestId);
    MDC.put(CLIENT_IP, clientIp);
    MDC.put(REQUEST_METHOD, request.getMethod());
    MDC.put(REQUEST_URI, request.getRequestURI());

    // 응답 헤더에 요청 ID/IP 추가
    response.setHeader(REQUEST_ID_HEADER, requestId);
    response.setHeader(REQUEST_IP_HEADER, clientIp);

    log.debug("Request started");
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) {
    // 요청 처리 후 MDC 데이터 정리
    log.debug("Request completed");
    MDC.clear();
  }
}
