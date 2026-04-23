package com.codeit.monew.domain.article.scheduler;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.entity.Keyword;
import com.codeit.monew.domain.interest.repository.KeywordRepository;
import com.codeit.monew.global.exception.external.ExternalClientException;
import com.codeit.monew.global.exception.external.ExternalNetworkException;
import com.codeit.monew.global.exception.external.ExternalRateLimitException;
import com.codeit.monew.global.exception.external.ExternalServerException;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NaverArticleBatchJobTest {

  @Mock
  private NaverKeywordTxProcessor naverKeywordTxProcessor;
  @Mock
  private KeywordRepository keywordRepository;

  @Spy
  @InjectMocks
  private NaverArticleBatchJob naverArticleBatchJob;

  private Keyword kw(String name) {
    return Keyword.create(Interest.create("I"), name);
  }

  private ExternalRateLimitException rateLimit() {
    return new ExternalRateLimitException(
        NewsSourceUrl.NAVER, "https://x", true, false, new RuntimeException("429"));
  }

  private ExternalServerException serverError() {
    return new ExternalServerException(
        NewsSourceUrl.NAVER, "https://x", 500, new RuntimeException("500"));
  }

  private ExternalNetworkException networkError() {
    return new ExternalNetworkException(
        NewsSourceUrl.NAVER, "https://x", new RuntimeException("network"));
  }

  @BeforeEach
  void setUp() {
    lenient().doNothing().when(naverArticleBatchJob).sleep(anyLong());
  }

  @Nested
  @DisplayName("run")
  class Run {

    @Test
    @DisplayName("키워드 로딩 시 blank 제거, trim, distinct 처리")
    void normalize_keywords_on_load() {
      // given: DB에서 조회된 키워드 목록에 중복("삼성", " 삼성 "), 빈 문자열(""), 공백("   ")이 섞여있다고 모킹
      given(keywordRepository.findAllWithInterest())
          .willReturn(List.of(kw("삼성"), kw(" 삼성 "), kw(""), kw("   ")));
      given(naverKeywordTxProcessor.processOneKeyword("삼성")).willReturn(1);

      // when: 네이버 기사 배치 잡 실행
      naverArticleBatchJob.run();

      // then: 공백 제거, 빈 문자열 필터링, 중복 제거가 적용되어 결과적으로 "삼성" 키워드에 대해서만 1회 호출되는지 검증
      verify(naverKeywordTxProcessor, times(1)).processOneKeyword("삼성");
    }

    @Test
    @DisplayName("키워드별 정상 성공")
    void success_per_keyword() {
      // given: 서로 다른 정상적인 2개의 키워드("삼성", "애플")가 조회되도록 모킹
      given(keywordRepository.findAllWithInterest()).willReturn(List.of(kw("삼성"), kw("애플")));
      given(naverKeywordTxProcessor.processOneKeyword("삼성")).willReturn(1);
      given(naverKeywordTxProcessor.processOneKeyword("애플")).willReturn(2);

      // when: 배치 잡 실행
      naverArticleBatchJob.run();

      // then: 각 키워드별로 한 번씩 순차적으로 프로세서가 정상 호출되는지 검증
      verify(naverKeywordTxProcessor).processOneKeyword("삼성");
      verify(naverKeywordTxProcessor).processOneKeyword("애플");
    }

    @Test
    @DisplayName("429 재시도 후 성공")
    void retry_rate_limit_then_success() {
      // given: "삼성" 키워드 처리 중 첫 번째 시도에서는 429 예외가 발생하고, 두 번째 시도에서는 정상적으로 반환(1)되도록 모킹
      given(keywordRepository.findAllWithInterest()).willReturn(List.of(kw("삼성")));
      given(naverKeywordTxProcessor.processOneKeyword("삼성"))
          .willThrow(rateLimit())
          .willReturn(1);

      // when: 배치 잡 실행
      naverArticleBatchJob.run();

      // then: 네이버 API의 429 에러는 즉시 재시도 대상이므로, 총 2회(실패 1회 + 성공 1회) 호출되었음을 검증
      verify(naverKeywordTxProcessor, times(2)).processOneKeyword("삼성");
    }

    @Test
    @DisplayName("429 재시도 소진 시 종료")
    void stop_when_rate_limit_retry_exhausted() {
      // given: "삼성" 처리 중 계속해서 429(Rate Limit) 예외만 발생하도록 모킹
      given(keywordRepository.findAllWithInterest()).willReturn(List.of(kw("삼성")));
      given(naverKeywordTxProcessor.processOneKeyword("삼성")).willThrow(rateLimit());

      // when: 배치 잡 실행
      naverArticleBatchJob.run();

      // then: 정해진 최대 재시도 횟수(최초 1회 + 재시도 5회 = 총 6회)에 도달하면 더 이상 시도하지 않고 종료됨을 검증
      verify(naverKeywordTxProcessor, times(6)).processOneKeyword("삼성");
    }

    @Test
    @DisplayName("server/network error 재시도 후 성공")
    void retry_and_success_on_server_error() {
      // given: "삼성" 키워드 처리 시 1, 2회차는 서버/네트워크 에러가 발생하고, 3회차에 성공하도록 모킹
      given(keywordRepository.findAllWithInterest()).willReturn(List.of(kw("삼성")));
      given(naverKeywordTxProcessor.processOneKeyword("삼성"))
          .willThrow(serverError())
          .willThrow(networkError())
          .willReturn(1);

      // when: 배치 잡 실행
      naverArticleBatchJob.run();

      // then: 총 3회(실패 2회 + 성공 1회) 호출되었음을 검증
      verify(naverKeywordTxProcessor, times(3)).processOneKeyword("삼성");
    }

    @Test
    @DisplayName("server/network 재시도 소진 시 종료")
    void stop_when_server_retry_exhausted() {
      // given: 서버 에러가 지속적으로 발생하도록 모킹
      given(keywordRepository.findAllWithInterest()).willReturn(List.of(kw("삼성")));
      given(naverKeywordTxProcessor.processOneKeyword("삼성"))
          .willThrow(serverError())
          .willThrow(networkError())
          .willThrow(serverError());

      // when: 배치 잡 실행
      naverArticleBatchJob.run();

      // then: 서버 에러 최대 재시도 횟수(최초 1회 + 재시도 3회 = 총 4회)를 소진한 뒤 종료됨을 검증
      verify(naverKeywordTxProcessor, times(4)).processOneKeyword("삼성");
    }

    @Test
    @DisplayName("client error 즉시 종료")
    void stop_immediately_on_client_error() {
      // given: 400번대 클라이언트 에러(ExternalClientException)가 발생하도록 모킹
      given(keywordRepository.findAllWithInterest()).willReturn(List.of(kw("삼성")));
      given(naverKeywordTxProcessor.processOneKeyword("삼성"))
          .willThrow(new ExternalClientException(
              NewsSourceUrl.NAVER, "https://x", 400, new RuntimeException("400")));

      // when: 배치 잡 실행
      naverArticleBatchJob.run();

      // then: 클라이언트 에러는 재시도해도 의미가 없으므로 재시도 로직을 타지 않고 1회만 호출 후 즉시 종료됨을 검증
      verify(naverKeywordTxProcessor).processOneKeyword("삼성");
    }

    @Test
    @DisplayName("예상치 못한 예외 발생 시 해당 키워드 종료")
    void stop_keyword_on_unexpected_error() {
      // given: 프로세스 도중 예상치 못한 일반 RuntimeException이 발생하도록 모킹
      given(keywordRepository.findAllWithInterest()).willReturn(List.of(kw("삼성")));
      given(naverKeywordTxProcessor.processOneKeyword("삼성"))
          .willThrow(new RuntimeException("boom"));

      // when: 배치 잡 실행
      naverArticleBatchJob.run();

      // then: 예측할 수 없는 예외 역시 무한 루프를 방지하기 위해 1회 호출로 종료되고 안전하게 넘어감을 검증
      verify(naverKeywordTxProcessor).processOneKeyword("삼성");
    }
  }
}