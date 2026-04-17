package com.codeit.monew.domain.article.controller;

import com.codeit.monew.domain.article.dto.ArticleDto;
import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.service.ArticleService;
import com.codeit.monew.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Tag(name = "뉴스 기사 관리", description = "뉴스 기사 관련 API")
public class ArticleController {

  private final ArticleService articleService;

  @GetMapping(value = "/{articleId}")
  @Operation(summary = "뉴스 기사 단건 조회", description = "뉴스 기사 ID로 뉴스 기사 단건을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ArticleDto.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 필수 헤더 누락", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "사용자 또는 뉴스 기사 정보 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<ArticleDto> getArticle(
      @Parameter(description = "뉴스 기사 ID") @PathVariable UUID articleId,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") UUID requestUserId
  ) {
    ArticleDto response = articleService.getArticle(articleId, requestUserId);

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @GetMapping(value = "/sources")
  @Operation(summary = "출처 목록 조회", description = "출처 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ArticleSource.class)))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<List<ArticleSource>> getSources() {
    List<ArticleSource> response = articleService.getSources();

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }
}
