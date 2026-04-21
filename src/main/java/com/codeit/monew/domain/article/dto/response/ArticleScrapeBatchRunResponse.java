package com.codeit.monew.domain.article.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record ArticleScrapeBatchRunResponse(
    @Schema(description = "Batch Job Execution ID")
    Long jobExecutionId,

    @Schema(description = "Batch Job 상태", example = "COMPLETED")
    String status,

    @Schema(description = "Batch Job 시작 시각")
    LocalDateTime startTime,

    @Schema(description = "Batch Job 종료 시각")
    LocalDateTime endTime,

    @Schema(description = "Step별 실행 결과")
    List<StepResult> steps
) {

  public record StepResult(
      @Schema(description = "Step 이름", example = "rssStep")
      String stepName,

      @Schema(description = "Step 상태", example = "COMPLETED")
      String status,

      @Schema(description = "Step 종료 코드", example = "COMPLETED")
      String exitCode,

      @Schema(description = "commit 횟수")
      long commitCount,

      @Schema(description = "rollback 횟수")
      long rollbackCount
  ) {

  }
}
