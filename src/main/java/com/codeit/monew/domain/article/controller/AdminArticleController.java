package com.codeit.monew.domain.article.controller;

import com.codeit.monew.domain.article.dto.ArticleScrapeRequest;
import com.codeit.monew.domain.article.dto.ArticleScrapeResponse;
import com.codeit.monew.domain.article.service.ArticleScrapeService;
import com.codeit.monew.infra.external.rss.NewsSourceUrl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
@Tag(name = "뉴스 기사 수집", description = "뉴스 기사 수집 테스트 API")
public class AdminArticleController {

  private final ArticleScrapeService articleScrapeService;

  @PostMapping("/scrape-test")
  @Operation(summary = "뉴스 기사 수집 테스트", description = "외부 RSS/네이버 API를 호출하여 기사를 수집하고 저장합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "수집 성공", content = @Content(schema = @Schema(implementation = ArticleScrapeResponse.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(implementation = Map.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  public ResponseEntity<?> scrapeTest(@RequestBody ArticleScrapeRequest request) {
    if (request == null || request.source() == null) {
      return badRequest("source는 필수입니다.");
    }

    if (request.source() == NewsSourceUrl.NAVER
        && (request.query() == null || request.query().isBlank())) {
      return badRequest("NAVER 요청에는 query가 필수입니다.");
    }

    int savedCount = articleScrapeService.scrapeAndSave(request.source(), request.query());
    ArticleScrapeResponse response = new ArticleScrapeResponse(
        request.source(),
        request.query(),
        savedCount
    );
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  private ResponseEntity<Map<String, Object>> badRequest(String message) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", Instant.now());
    body.put("code", "INVALID_REQUEST");
    body.put("message", message);
    body.put("status", HttpStatus.BAD_REQUEST.value());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }
}

