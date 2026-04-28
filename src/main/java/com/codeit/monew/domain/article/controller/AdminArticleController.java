package com.codeit.monew.domain.article.controller;

import com.codeit.monew.domain.article.dto.response.ArticleScrapeBatchRunResponse;
import com.codeit.monew.domain.article.scheduler.backup.ArticleBackupBatchRunner;
import com.codeit.monew.domain.article.service.ArticleScrapeBatchRunner;
import com.codeit.monew.domain.article.service.ArticleScrapeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
@Tag(name = "뉴스 기사 수집", description = "뉴스 기사 수집 테스트 API")
public class AdminArticleController {

  private final ArticleScrapeService articleScrapeService;
  private final ArticleScrapeBatchRunner articleScrapeBatchRunner;
  private final ArticleBackupBatchRunner articleBackupBatchRunner;

  @PostMapping("/scrape-batch/run")
  @Operation(summary = "뉴스 수집 배치 수동 실행", description = "Spring Batch 뉴스 수집 Job을 즉시 실행하고 Step별 결과를 반환합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "배치 실행 완료",
          content = @Content(schema = @Schema(implementation = ArticleScrapeBatchRunResponse.class))),
      @ApiResponse(responseCode = "500", description = "배치 실행 중 서버 오류")
  })
  public ResponseEntity<?> runScrapeBatch() {
    try {
      ArticleScrapeBatchRunResponse response = articleScrapeBatchRunner.runNow();
      return ResponseEntity.ok(response);
    } catch (JobExecutionException e) {
      Map<String, Object> body = new HashMap<>();
      body.put("timestamp", Instant.now());
      body.put("code", "BATCH_EXECUTION_FAILED");
      body.put("message", "뉴스 수집 배치 실행에 실패했습니다.");
      body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
  }

  private ResponseEntity<Map<String, Object>> badRequest(String message) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", Instant.now());
    body.put("code", "INVALID_REQUEST");
    body.put("message", message);
    body.put("status", HttpStatus.BAD_REQUEST.value());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  // main branch 병합 시 삭제 예정
  @PostMapping("/backup-batch-restore-test/run/{day}")
  public ResponseEntity backupTest(@PathVariable int day) {
    ZoneId KST = ZoneId.of("Asia/Seoul");

    LocalDate backupDate;
    if (day == 0) {
      backupDate = LocalDate.now(KST);
    } else {
      backupDate = LocalDate.now(KST).minusDays(day);
    }

    log.debug("[ARTICLE_BACKUP_TEST] 뉴스 기사 백업 테스트 시작: day={}, backupDate={}", day, backupDate);

    articleBackupBatchRunner.run(backupDate);

    log.debug("[ARTICLE_BACKUP_TEST] 뉴스 기사 백업 테스트 완료: backupDate={}", backupDate);

    return ResponseEntity.status(HttpStatus.OK).build();
  }
}
