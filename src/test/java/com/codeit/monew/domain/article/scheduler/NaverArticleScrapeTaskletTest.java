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

  @Mock
  private BatchCircuitBreaker circuitBreaker;

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
  @DisplayName("키워드 정규화(공백 제거/중복 제거/빈 값 필터링) 후 성공 결과를 합산하여 저장한다")
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
    verify(circuitBreaker, times(1)).recordFailure(); // 삼성 실패
    verify(circuitBreaker, times(1)).recordSuccess(); // 애플 성공

    // 성공한 "애플"의 결과(2)만 누적되어 저장되었는지 확인
    ArgumentCaptor<ArticleScrapeResult> captor = ArgumentCaptor.forClass(ArticleScrapeResult.class);
    verify(contextManager, times(1)).merge(any(ChunkContext.class), captor.capture());
    assertEquals(2, captor.getValue().totalSavedCount());
  }

  @Test
  @DisplayName("서킷 브레이커가 열려 있으면 작업을 실행하지 않고 조기에 종료한다")
  void execute_stops_when_circuit_is_broken() {
    // Given: 이미 서킷 브레이커가 끊어진(Broken) 상태
    when(keywordRepository.findAllWithInterest()).thenReturn(List.of(kw("삼성"), kw("애플")));
    when(circuitBreaker.isBroken()).thenReturn(true);
    when(circuitBreaker.getConsecutiveFailures()).thenReturn(10);

    // When: Tasklet 실행
    RepeatStatus status = tasklet.execute(contribution, chunkContext);

    // Then: 어떠한 Job 실행이나 성공/실패 기록도 호출되지 않아야 함
    assertEquals(RepeatStatus.FINISHED, status);
    verify(naverArticleBatchJob, never()).run(any());
    verify(circuitBreaker, never()).recordSuccess();
    verify(circuitBreaker, never()).recordFailure();

    // 빈 결과 객체가 merge되어야 함
    ArgumentCaptor<ArticleScrapeResult> captor = ArgumentCaptor.forClass(ArticleScrapeResult.class);
    verify(contextManager, times(1)).merge(any(ChunkContext.class), captor.capture());
    assertEquals(0, captor.getValue().totalSavedCount());
  }

  @Test
  @DisplayName("처리 로직 밖의 예상치 못한 예외는 전파하고 머지하지 않는다")
  void execute_propagates_unknown_exception() {
    // Given: 비즈니스 예외(ExternalApiException)가 아닌 일반 예외 발생 상황
    when(keywordRepository.findAllWithInterest()).thenReturn(List.of(kw("삼성")));
    when(circuitBreaker.isBroken()).thenReturn(false);
    when(naverArticleBatchJob.run("삼성")).thenThrow(new RuntimeException("boom"));

    // When & Then: 예외가 상위로 전파되는지 확인
    assertThrows(RuntimeException.class, () -> tasklet.execute(contribution, chunkContext));

    // 예외 발생 시 서킷 브레이커 기록이나 결과 저장 로직이 수행되지 않아야 함
    verify(contextManager, never()).merge(any(ChunkContext.class), any(ArticleScrapeResult.class));
    verify(circuitBreaker, never()).recordSuccess();
    verify(circuitBreaker, never()).recordFailure();
  }

  /**
   * 키워드 엔티티 생성을 위한 편의 메서드
   */
  private Keyword kw(String name) {
    return Keyword.create(Interest.create("I"), name);
  }
}