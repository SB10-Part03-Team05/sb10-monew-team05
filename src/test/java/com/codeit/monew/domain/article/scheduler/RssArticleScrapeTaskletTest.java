package com.codeit.monew.domain.article.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

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
  @DisplayName("RSS 소스만 실행하고 NAVER는 건너뛰며 전체 결과를 병합한다")
  void execute_runs_non_naver_sources_and_merges() {
    // Given: 각 언론사별(RSS) 실행 결과 설정 (NAVER 제외)
    when(rssArticleBatchJob.run(NewsSourceUrl.HANKYUNG))
        .thenReturn(new ArticleScrapeResult(1, Map.of()));
    when(rssArticleBatchJob.run(NewsSourceUrl.CHOSUN))
        .thenReturn(new ArticleScrapeResult(2, Map.of()));
    when(rssArticleBatchJob.run(NewsSourceUrl.YONHAP))
        .thenReturn(new ArticleScrapeResult(3, Map.of()));

    // When: Tasklet 실행
    RepeatStatus status = tasklet.execute(contribution, chunkContext);

    // Then: 실행 상태 확인 및 소스별 호출 여부 검증
    assertEquals(RepeatStatus.FINISHED, status);

    // 각 RSS 소스는 1회씩 호출되어야 함
    verify(rssArticleBatchJob, times(1)).run(NewsSourceUrl.HANKYUNG);
    verify(rssArticleBatchJob, times(1)).run(NewsSourceUrl.CHOSUN);
    verify(rssArticleBatchJob, times(1)).run(NewsSourceUrl.YONHAP);

    // NAVER 소스는 RSS Tasklet에서 호출되지 않아야 함 (필터링 검증)
    verify(rssArticleBatchJob, never()).run(NewsSourceUrl.NAVER);
    verifyNoMoreInteractions(rssArticleBatchJob);

    // Context Manager에 최종 합계(1+2+3=6)가 전달되었는지 확인
    ArgumentCaptor<ArticleScrapeResult> captor = ArgumentCaptor.forClass(ArticleScrapeResult.class);
    verify(contextManager, times(1)).merge(any(ChunkContext.class), captor.capture());
    assertEquals(6, captor.getValue().totalSavedCount());
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

    // 에러가 발생한 이후의 소스(예: YONHAP 등)는 실행되지 않았어야 함
    verify(rssArticleBatchJob, never()).run(NewsSourceUrl.YONHAP);
  }
}
