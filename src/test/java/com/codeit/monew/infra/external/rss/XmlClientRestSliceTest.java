package com.codeit.monew.infra.external.rss;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.codeit.monew.global.exception.external.ExternalClientException;
import com.codeit.monew.global.exception.external.ExternalEmptyResponseException;
import com.codeit.monew.global.exception.external.ExternalNetworkException;
import com.codeit.monew.global.exception.external.ExternalRateLimitException;
import com.codeit.monew.global.exception.external.ExternalServerException;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@RestClientTest(
    value = XmlClient.class,
    properties = {
        "monew.naver.client-id=test-id",
        "monew.naver.client-secret=test-secret"
    }
)
@Import(XmlClientRestSliceTest.TestConfig.class)
class XmlClientRestSliceTest {

  static class TestConfig {

    @Bean
    RestClient restClient(RestClient.Builder builder) {
      return builder.build();
    }
  }

  @Autowired
  private XmlClient xmlClient;

  @Autowired
  private MockRestServiceServer server;

  @Nested
  @DisplayName("정상 응답")
  class SuccessCases {

    @Test
    @DisplayName("RSS 요청 성공")
    void success_fetch_rss_xml() {
      // given: 특정 언론사 RSS URL로 GET 요청이 들어올 때 성공(200) 응답을 반환하도록 모킹
      server.expect(requestTo(NewsSourceUrl.CHOSUN.resolveRssUrl()))
          .andExpect(method(GET))
          .andRespond(withSuccess("<rss></rss>", MediaType.APPLICATION_XML));

      // when: 실제 XmlClient를 통해 RSS 데이터를 요청
      String xml = xmlClient.fetchRssXml(NewsSourceUrl.CHOSUN);

      // then: 반환된 XML 결과값이 모킹한 응답과 일치하는지 검증
      assertEquals("<rss></rss>", xml);
    }

    @Test
    @DisplayName("NAVER 요청 성공 및 인증 헤더 포함")
    void success_fetch_naver_xml_with_headers() {
      // given: 네이버 Open API 요청 시 필수 인증 헤더가 포함되어 있는지 검증하고, 성공(200) 응답을 반환하도록 모킹
      server.expect(requestTo(containsString("openapi.naver.com/v1/search/news.xml")))
          .andExpect(method(GET))
          .andExpect(header("X-Naver-Client-Id", "test-id"))
          .andExpect(header("X-Naver-Client-Secret", "test-secret"))
          .andRespond(withSuccess("<rss></rss>", MediaType.APPLICATION_XML));

      // when: 검색어("삼성")를 이용해 네이버 뉴스 XML 데이터를 요청
      String xml = xmlClient.fetchNaverXml("삼성");

      // then: 반환된 XML 결과값이 모킹한 응답과 일치하는지 검증
      assertEquals("<rss></rss>", xml);
    }
  }

  @Nested
  @DisplayName("오류 응답 매핑")
  class ErrorCases {

    @Test
    @DisplayName("빈/null 응답은 ExternalEmptyResponseException")
    void throw_when_empty_response() {
      // given: 외부 API가 빈 문자열(내용 없음)로 성공 응답을 반환하도록 모킹
      server.expect(requestTo(NewsSourceUrl.CHOSUN.resolveRssUrl()))
          .andRespond(withSuccess("", MediaType.APPLICATION_XML));

      // when & then: 빈 응답일 경우 ExternalEmptyResponseException이 발생하는지 검증
      assertThrows(ExternalEmptyResponseException.class,
          () -> xmlClient.fetchRssXml(NewsSourceUrl.CHOSUN));
    }

    @Test
    @DisplayName("429 NAVER는 즉시 재시도 가능 예외")
    void throw_rate_limit_exception_for_naver() {
      // given: 네이버 뉴스 API 요청에 대해 429(Too Many Requests) 상태 코드를 반환하도록 모킹
      server.expect(requestTo(containsString("openapi.naver.com/v1/search/news.xml")))
          .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

      // when: 예외가 발생하는 로직을 실행하고 해당 예외를 캡처
      ExternalRateLimitException ex = assertThrows(ExternalRateLimitException.class,
          () -> xmlClient.fetchNaverXml("네이버"));

      // then: 네이버 API의 429 에러는 즉시 재시도 가능하고(isRetryable = true), 다음 배치 재시도가 아님을 검증
      assertTrue(ex.isRetryable());
      assertFalse(ex.isRetryNextBatch());
    }

    @Test
    @DisplayName("429 RSS는 다음 배치 재시도 예외")
    void throw_rate_limit_exception_for_rss() {
      // given: 일반 RSS 요청에 대해 429(Too Many Requests) 상태 코드를 반환하도록 모킹
      server.expect(requestTo(NewsSourceUrl.CHOSUN.resolveRssUrl()))
          .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

      // when: 예외가 발생하는 로직을 실행하고 해당 예외를 캡처
      ExternalRateLimitException ex = assertThrows(ExternalRateLimitException.class,
          () -> xmlClient.fetchRssXml(NewsSourceUrl.CHOSUN));

      // then: RSS의 429 에러는 즉시 재시도가 불가하며(isRetryable = false), 다음 배치에서 재시도해야 함을 검증
      assertFalse(ex.isRetryable());
      assertTrue(ex.isRetryNextBatch());
    }

    @Test
    @DisplayName("429 제외 4xx는 ExternalClientException")
    void throw_client_exception_for_4xx() {
      // given: RSS 요청에 대해 400(Bad Request) 상태 코드를 반환하도록 모킹
      server.expect(requestTo(NewsSourceUrl.CHOSUN.resolveRssUrl()))
          .andRespond(withStatus(HttpStatus.BAD_REQUEST));

      // when & then: 4xx 클라이언트 에러 발생 시 ExternalClientException으로 매핑되는지 검증
      assertThrows(ExternalClientException.class,
          () -> xmlClient.fetchRssXml(NewsSourceUrl.CHOSUN));
    }

    @Test
    @DisplayName("5xx는 ExternalServerException")
    void throw_server_exception_for_5xx() {
      // given: RSS 요청에 대해 500(Internal Server Error) 상태 코드를 반환하도록 모킹
      server.expect(requestTo(NewsSourceUrl.CHOSUN.resolveRssUrl()))
          .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

      // when & then: 5xx 서버 에러 발생 시 ExternalServerException으로 매핑되는지 검증
      assertThrows(ExternalServerException.class,
          () -> xmlClient.fetchRssXml(NewsSourceUrl.CHOSUN));
    }

    @Test
    @DisplayName("네트워크 오류는 ExternalNetworkException")
    void throw_network_exception() {
      // given: 서버 통신 중 네트워크 수준의 예외(IOException)가 발생하도록 모킹
      server.expect(requestTo(NewsSourceUrl.CHOSUN.resolveRssUrl()))
          .andRespond(withException(new IOException("boom")));

      // when & then: 네트워크 오류 발생 시 ExternalNetworkException으로 매핑되는지 검증
      assertThrows(ExternalNetworkException.class,
          () -> xmlClient.fetchRssXml(NewsSourceUrl.CHOSUN));
    }
  }
}