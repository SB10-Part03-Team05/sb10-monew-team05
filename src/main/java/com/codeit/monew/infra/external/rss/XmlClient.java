package com.codeit.monew.infra.external.rss;

import com.codeit.monew.global.exception.external.client.ExternalClientException;
import com.codeit.monew.global.exception.external.client.ExternalEmptyResponseException;
import com.codeit.monew.global.exception.external.client.ExternalNetworkException;
import com.codeit.monew.global.exception.external.client.ExternalRateLimitException;
import com.codeit.monew.global.exception.external.client.ExternalServerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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

  public String fetchArticleHtml(NewsSourceUrl source, String articleUrl) {
    return request(source, articleUrl);
  }

  private String request(NewsSourceUrl source, String url) {
    try {
      String xml = restClient.get()
          .uri(url)
          .headers(headers -> {
            headers.set("User-Agent", "Mozilla/5.0");
            if (source.isNaver()) {
              headers.set("X-Naver-Client-Id", naverClientId);
              headers.set("X-Naver-Client-Secret", naverClientSecret);
            }
          })
          .retrieve()
          .body(String.class);

      if (xml == null || xml.isBlank()) {
        throw new ExternalEmptyResponseException(source, url);
      }
      return xml;

    } catch (HttpClientErrorException.TooManyRequests e) {
      throw new ExternalRateLimitException(source, url, e.getStatusCode(), e);
    } catch (HttpClientErrorException e) {
      throw new ExternalClientException(source, url, e.getStatusCode(), e);
    } catch (HttpServerErrorException e) {
      throw new ExternalServerException(source, url, e.getStatusCode(), e);
    } catch (RestClientException e) {
      throw new ExternalNetworkException(source, url, e);
    }
  }
}
