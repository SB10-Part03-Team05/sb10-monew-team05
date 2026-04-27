package com.codeit.monew.domain.article.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.entity.Keyword;
import com.codeit.monew.domain.interest.repository.KeywordRepository;
import com.codeit.monew.global.exception.external.ExternalNetworkException;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

@ExtendWith(MockitoExtension.class)
class NaverArticleScrapeTaskletTest {

  @Mock
  private NaverArticleBatchJob naverArticleBatchJob;

  @Mock
  private KeywordRepository keywordRepository;

  @Spy
  private BatchCircuitBreaker circuitBreaker = new BatchCircuitBreaker();

  @Mock
  private ArticleScrapeResultExecutionContextManager contextManager;

  @Spy
  @InjectMocks
  private NaverArticleScrapeTasklet tasklet;

  @Mock
  private StepContribution contribution;

  @Mock
  private ChunkContext chunkContext;

  @BeforeEach
  void setUp() {
    // 테스트 속도를 위해 sleep 메서드는 실제 동작하지 않도록 설정
    lenient().doNothing().when(tasklet).sleep(anyLong());
  }

  @Test
  @DisplayName("키워드 정규화(공백 제거/중복 제거/빈 값 필터링) 후 성공 결과를 합산하여 머지한다")
  void execute_success_path_with_normalized_keywords() {
    // Given: 중복되고 공백이 포함된 다양한 키워드 목록 준비
    when(keywordRepository.findAllWithInterest()).thenReturn(
        List.of(kw(" 삼성 "), kw("삼성"), kw(""), kw("   "), kw("애플")));
    when(circuitBreaker.isBroken()).thenReturn(false, false);

    // 정규화된 결과인 "삼성"과 "애플"에 대해서만 동작 정의
    when(naverArticleBatchJob.run("삼성")).thenReturn(new ArticleScrapeResult(1, Map.of()));
    when(naverArticleBatchJob.run("애플")).thenReturn(new ArticleScrapeResult(2, Map.of()));

    // When: Tasklet 실행
    RepeatStatus status = tasklet.execute(contribution, chunkContext);

    // Then: 실행 상태 및 호출 내역 검증
    assertEquals(RepeatStatus.FINISHED, status);
    verify(circuitBreaker, times(1)).reset(); // 시작 시 초기화 확인

    // 정규화되어 최종적으로 "삼성", "애플" 1회씩만 호출되었는지 확인
    verify(naverArticleBatchJob, times(1)).run("삼성");
    verify(naverArticleBatchJob, times(1)).run("애플");
    verify(circuitBreaker, times(2)).recordSuccess();
    verify(circuitBreaker, never()).recordFailure();

    // 최종 결과가 1+2=3으로 병합되었는지 검증
    ArgumentCaptor<ArticleScrapeResult> captor = ArgumentCaptor.forClass(ArticleScrapeResult.class);
    verify(contextManager, times(1)).merge(any(ChunkContext.class), captor.capture());
    assertEquals(3, captor.getValue().totalSavedCount());
  }

  @Test
  @DisplayName("외부 API 예외 발생 시 해당 키워드는 건너뛰고 다음 키워드를 계속 처리한다")
  void execute_external_api_exception_continue() {
    // Given: "삼성" 처리 시 예외 발생, "애플"은 성공하는 상황 설정
    when(keywordRepository.findAllWithInterest()).thenReturn(List.of(kw("삼성"), kw("애플")));
    when(circuitBreaker.isBroken()).thenReturn(false, false);
    when(naverArticleBatchJob.run("삼성"))
        .thenThrow(new ExternalNetworkException(NewsSourceUrl.NAVER, "https://x",
            new RuntimeException("network")));
    when(naverArticleBatchJob.run("애플")).thenReturn(new ArticleScrapeResult(2, Map.of()));

    // When: Tasklet 실행
    RepeatStatus status = tasklet.execute(contribution, chunkContext);

    // Then: 한 번의 실패와 한 번의 성공이 기록되어야 함
    assertEquals(RepeatStatus.FINISHED, status);
    verify(naverArticleBatchJob, times(1)).run("삼성");
    verify(naverArticleBatchJob, times(1)).run("애플");
    verify(circuitBreaker, times(1)).recordFailure(); // 삼성 실패
    verify(circuitBreaker, times(1)).recordSuccess(); // 애플 성공

    // 성공한 "애플"의 결과(2)만 누적되어 저장되었는지 확인
    ArgumentCaptor<ArticleScrapeResult> captor = ArgumentCaptor.forClass(ArticleScrapeResult.class);
    verify(contextManager, times(1)).merge(any(ChunkContext.class), captor.capture());
    assertEquals(2, captor.getValue().totalSavedCount());
  }

  @Test
  @DisplayName("서킷 브레이커가 10회 연속 실패로 열리면 남은 키워드를 조기 종료한다")
  void execute_stops_when_circuit_is_broken() {
    // Given: 11개 키워드 중 앞 10개가 연속 실패하도록 설정
    when(keywordRepository.findAllWithInterest()).thenReturn(List.of(
        kw("k1"), kw("k2"), kw("k3"), kw("k4"), kw("k5"),
        kw("k6"), kw("k7"), kw("k8"), kw("k9"), kw("k10"), kw("k11")
    ));
    when(naverArticleBatchJob.run(any()))
        .thenThrow(new ExternalNetworkException(
            NewsSourceUrl.NAVER, "https://x", new RuntimeException("network")));

    // When: Tasklet 실행
    RepeatStatus status = tasklet.execute(contribution, chunkContext);

    // Then: 10회 실패 후 서킷이 열려 11번째 키워드는 실행되지 않아야 함
    assertEquals(RepeatStatus.FINISHED, status);
    verify(naverArticleBatchJob, times(10)).run(any());
    verify(naverArticleBatchJob, never()).run("k11");
    verify(circuitBreaker, times(10)).recordFailure();
    verify(circuitBreaker, never()).recordSuccess();

    // 빈 결과 객체가 merge되어야 함
    ArgumentCaptor<ArticleScrapeResult> captor = ArgumentCaptor.forClass(ArticleScrapeResult.class);
    verify(contextManager, times(1)).merge(any(ChunkContext.class), captor.capture());
    assertEquals(0, captor.getValue().totalSavedCount());
  }

  @Test
  @DisplayName("처리 로직 밖의 예외 발생 시, 예외는 전파되지만 이전까지의 결과는 merge한다")
  void execute_propagates_exception_but_merges_partial_result() {
    // Given: "삼성"은 성공하고, "애플"에서 예상치 못한 에러가 발생하는 상황
    when(keywordRepository.findAllWithInterest()).thenReturn(List.of(kw("삼성"), kw("애플")));
    when(circuitBreaker.isBroken()).thenReturn(false);

    // 첫 번째 키워드는 성공하여 기사 1개를 가져옴
    when(naverArticleBatchJob.run("삼성")).thenReturn(new ArticleScrapeResult(1, Map.of()));
    // 두 번째 키워드에서 런타임 에러 발생
    when(naverArticleBatchJob.run("애플")).thenThrow(new RuntimeException("boom"));

    // When & Then: 예외는 상위로 던져져야 함
    assertThrows(RuntimeException.class, () -> tasklet.execute(contribution, chunkContext));

    // 예외가 터지기 전까지 수집된 "삼성(1개)"의 결과가 merge되었는지 확인
    ArgumentCaptor<ArticleScrapeResult> captor = ArgumentCaptor.forClass(ArticleScrapeResult.class);
    verify(contextManager, times(1)).merge(any(ChunkContext.class), captor.capture());

    // 에러 발생 전까지 저장된 기사의 개수인 totalSavedCount가 1인지 확인
    assertEquals(1, captor.getValue().totalSavedCount());

    // 서킷 브레이커는 성공한 "삼성"에 대해서만 기록됨
    verify(circuitBreaker, times(1)).recordSuccess();
    verify(circuitBreaker, never()).recordFailure(); // RuntimeException은 서킷 브레이커 대상이 아님
  }

  /**
   * 키워드 엔티티 생성을 위한 편의 메서드
   */
  private Keyword kw(String name) {
    return Keyword.create(Interest.create("I"), name);
  }
}