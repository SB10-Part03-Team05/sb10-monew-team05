package com.codeit.monew.domain.article.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;

@ExtendWith(MockitoExtension.class)
class RssArticleScrapeTaskletTest {

  @Mock
  private RssArticleBatchJob rssArticleBatchJob;

  @Mock
  private ArticleScrapeResultExecutionContextManager contextManager;

  @InjectMocks
  private RssArticleScrapeTasklet tasklet;

  @Mock
  private StepContribution contribution;

  @Mock
  private ChunkContext chunkContext;

  @Test
  @DisplayName("NAVER를 제외한 모든 소스를 순서대로 실행하며, 에러 발생 시 이후 소스는 건너뛰고 결과를 병합한다")
  void execute_runs_non_naver_sources_dynamically_until_error() {
    // 1. Given: 전체 소스 중 NAVER를 제외한 실행 대상 리스트 준비
    List<NewsSourceUrl> targetSources = Arrays.stream(NewsSourceUrl.values())
        .filter(source -> source != NewsSourceUrl.NAVER) // NAVER는 RSS 배치가 아니므로 제외
        .toList();

    NewsSourceUrl errorSource = NewsSourceUrl.YNA; // 에러가 발생할 지점
    int expectedTotalCount = 0;
    int countPerSource = 1;

    for (NewsSourceUrl source : targetSources) {
      if (source == errorSource) {
        // 에러 지점 모킹
        when(rssArticleBatchJob.run(source)).thenThrow(new RuntimeException("fatal"));
        break;
      }
      // 정상 지점 모킹 및 예상 결과 누적
      expectedTotalCount += countPerSource;
      when(rssArticleBatchJob.run(source))
          .thenReturn(new ArticleScrapeResult(countPerSource, Map.of()));
    }

    // 2. When: Tasklet 실행 및 예외 확인
    assertThrows(RuntimeException.class, () -> tasklet.execute(contribution, chunkContext));

    // 3. Then: 호출 순서 및 제외 대상 검증
    InOrder inOrder = inOrder(rssArticleBatchJob);

    for (NewsSourceUrl source : targetSources) {
      inOrder.verify(rssArticleBatchJob).run(source);
      if (source == errorSource) {
        break; // 에러 지점까지만 호출 확인
      }
    }

    // [중요] NAVER는 아예 호출되지 않았음을 보장
    verify(rssArticleBatchJob, never()).run(NewsSourceUrl.NAVER);

    // 에러 발생 이후의 소스들이 호출되지 않았음을 보장
    verifyNoMoreInteractions(rssArticleBatchJob);

    // Context Manager에 에러 발생 전까지의 합산 결과(expectedTotalCount)가 잘 전달되었는지 확인
    ArgumentCaptor<ArticleScrapeResult> captor = ArgumentCaptor.forClass(ArticleScrapeResult.class);
    verify(contextManager).merge(any(ChunkContext.class), captor.capture());
    assertEquals(expectedTotalCount, captor.getValue().totalSavedCount());
  }

  @Test
  @DisplayName("RSS 소스 처리 중 예외가 발생해도, 직전까지 성공한 결과는 병합(Merge)하고 예외를 전파한다")
  void execute_propagates_exception_but_merges_partial_result() {
    // Given: 첫 번째 소스(HANKYUNG)는 성공하고, 두 번째 소스(CHOSUN)에서 예외가 발생하는 상황
    // 첫 번째 소스 실행 결과: 기사 1개 저장
    when(rssArticleBatchJob.run(NewsSourceUrl.HANKYUNG))
        .thenReturn(new ArticleScrapeResult(1, Map.of()));

    // 두 번째 소스 실행 중 예상치 못한 런타임 에러 발생
    when(rssArticleBatchJob.run(NewsSourceUrl.CHOSUN))
        .thenThrow(new RuntimeException("RSS 소스 실행 중 에러 발생!"));

    // When & Then: 예외가 상위로 전파되는지 확인 (Tasklet을 빠져나감)
    assertThrows(RuntimeException.class, () -> tasklet.execute(contribution, chunkContext));

    // 에러가 터졌음에도 불구하고, 직전까지 성공한 'HANKYUNG'의 결과(1개)가 머지되었는가?
    ArgumentCaptor<ArticleScrapeResult> captor = ArgumentCaptor.forClass(ArticleScrapeResult.class);
    verify(contextManager, times(1)).merge(any(ChunkContext.class), captor.capture());

    // 에러 발생 전까지 저장된 기사의 개수인 totalSavedCount가 1인지 확인
    assertEquals(1, captor.getValue().totalSavedCount());

    // 에러가 발생한 이후의 소스(예: YONHAP 등)는 실행되지 않았어야 함
    verify(rssArticleBatchJob, never()).run(NewsSourceUrl.YONHAP);
  }
}
