package com.codeit.monew.infra.external.rss;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class XmlClient {

  private final RestClient restClient;

  @Value("${monew.naver.client-id:}")
  private String naverClientId;

  @Value("${monew.naver.client-secret:}")
  private String naverClientSecret;

  public String fetchRssXml(NewsSourceUrl source) {
    String url = source.resolveRssUrl();
    return request(source, url);
  }

  public String fetchNaverXml(String query) {
    String url = NewsSourceUrl.NAVER.resolveNaverUrl(query);
    return request(NewsSourceUrl.NAVER, url);
  }

  private String request(NewsSourceUrl source, String url) {
    String xml = restClient.get()
        .uri(url)
        .headers(headers -> {
          headers.set("User-Agent", "Mozilla/5.0");
          // 네이버의 경우 Header에 API 사용 신청에서 발급받은 Client ID와 Secret Key를 넣어줘야 함
          if (source.isNaver()) {
            headers.set("X-Naver-Client-Id", naverClientId);
            headers.set("X-Naver-Client-Secret", naverClientSecret);
          }
        })
        .retrieve()
        .body(String.class);

    if (xml == null || xml.isBlank()) {
      // todo: 커스텀 예외로 전환 필요
      throw new IllegalStateException("XML 응답이 비어 있습니다. source=" + source);
    }
    return xml;
  }
}
