package com.codeit.monew.domain.article.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;

class ArticleScrapeResultExecutionContextManagerTest {

  private final ArticleScrapeResultExecutionContextManager manager =
      new ArticleScrapeResultExecutionContextManager();

  @Test
  @DisplayName("Job 실행 컨텍스트에 결과가 없으면 null을 반환한다")
  void get_returns_null_when_absent() {
    // Given: 아무런 결과가 저장되지 않은 초기 상태의 컨텍스트 생성
    ChunkContext chunkContext = chunkContext();

    // When: 결과를 조회 시도
    ArticleScrapeResult result = manager.get(chunkContext);

    // Then: 반환된 결과가 null이어야 함
    assertNull(result);
  }

  @Test
  @DisplayName("기존 값이 없을 때 첫 번째 결과를 저장한다")
  void merge_puts_first_result() {
    // Given: 컨텍스트와 저장할 첫 번째 결과 준비
    ChunkContext chunkContext = chunkContext();
    ArticleScrapeResult first = new ArticleScrapeResult(2, Map.of());

    // When: 결과를 컨텍스트에 병합(저장)
    manager.merge(chunkContext, first);

    // Then: 저장된 결과가 이전에 넣은 데이터와 일치하는지 확인
    assertEquals(first, manager.get(chunkContext));
  }

  @Test
  @DisplayName("기존 값이 존재하면 plus 메서드를 사용하여 결과를 누적한다")
  void merge_accumulates_existing_result() {
    // Given: 컨텍스트와 두 개의 순차적인 결과 준비 (동일 관심사 포함)
    ChunkContext chunkContext = chunkContext();
    UUID sameInterest = UUID.randomUUID();
    UUID anotherInterest = UUID.randomUUID();

    ArticleScrapeResult first = new ArticleScrapeResult(
        2,
        Map.of(sameInterest, new ArticleScrapeResult.InterestInfo("IT", 2))
    );
    ArticleScrapeResult second = new ArticleScrapeResult(
        3,
        Map.of(
            sameInterest, new ArticleScrapeResult.InterestInfo("IT", 1),
            anotherInterest, new ArticleScrapeResult.InterestInfo("AUTO", 2)
        )
    );

    // When: 첫 번째 결과 저장 후 두 번째 결과를 병합
    manager.merge(chunkContext, first);
    manager.merge(chunkContext, second);
    ArticleScrapeResult merged = manager.get(chunkContext);

    // Then: 전체 합계와 관심사별 개수가 합산되었는지 검증
    assertEquals(5, merged.totalSavedCount()); // 2 + 3
    assertEquals(2, merged.results().size());   // IT, AUTO 두 종류
    assertEquals(3, merged.results().get(sameInterest).count()); // 2 + 1
    assertEquals(2, merged.results().get(anotherInterest).count()); // 0 + 2
  }

  /**
   * 테스트를 위한 Spring Batch ChunkContext 모킹 유틸리티
   */
  private ChunkContext chunkContext() {
    JobExecution jobExecution = new JobExecution(1L);
    StepExecution stepExecution = new StepExecution("step", jobExecution);
    StepContext stepContext = new StepContext(stepExecution);
    return new ChunkContext(stepContext);
  }
}