package com.codeit.monew.domain.article.controller;

import com.codeit.monew.domain.article.dto.ArticleDto;
import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.dto.CursorPageResponseArticleDto;
import com.codeit.monew.domain.article.entity.type.ArticleDirection;
import com.codeit.monew.domain.article.entity.type.ArticleOrderBy;
import com.codeit.monew.domain.article.service.ArticleService;
import com.codeit.monew.global.exception.ErrorResponse;
import com.codeit.monew.global.exception.common.InvalidParameterException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  @GetMapping
  @Operation(summary = "뉴스 기사 목록 조회", description = "조건에 맞는 뉴스 기사 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = CursorPageResponseArticleDto.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (정렬 기준 오류, 페이지네이션 파라미터 오류 등)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "사용자 또는 관심사 정보 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<CursorPageResponseArticleDto> search(
      @Parameter(description = "검색어(제목, 요약)") @RequestParam(required = false) String keyword,
      @Parameter(description = "관심사 ID") @RequestParam(required = false) UUID interestId,
      @Parameter(description = "출처(포함)") @RequestParam(required = false) List<ArticleSource> sourceIn,
      @Parameter(description = "날짜 시작(범위)") @RequestParam(required = false) Instant publishDateFrom,
      @Parameter(description = "날짜 끝(범위)") @RequestParam(required = false) Instant publishDateTo,
      @Parameter(description = "정렬 속성 이름") @RequestParam ArticleOrderBy orderBy,
      @Parameter(description = "정렬 방향 (ASC, DESC)") @RequestParam ArticleDirection direction,
      @Parameter(description = "커서 값") @RequestParam(required = false) String cursor,
      @Parameter(description = "보조 커서(createdAt) 값") @RequestParam(required = false) Instant after,
      @Parameter(description = "커서 페이지 크기") @RequestParam int limit,
      @Parameter(description = "요청자 ID") @RequestHeader("Monew-Request-User-ID") UUID requestUserId
  ) {
    // keyword 정규화
    // `strip` 이 `trim` 보다 `\n`(줄바꿈) `\t`(탭) 같은 유니코드 공백까지 잘 처리. 단, java 11이상
    keyword = keyword == null ? null : keyword.strip();

    // keyword(검색어) 화이트 스페이스 검증
    if (keyword != null && keyword.isEmpty()) {
      throw new InvalidParameterException("keyword", keyword);
    }

    // 날짜 시작일은 날짜 종료일보다 늦을 수 없음
    if (publishDateFrom != null && publishDateTo != null && publishDateFrom.isAfter(
        publishDateTo)) {
      throw new InvalidParameterException("publishDateFrom", publishDateFrom, "publishDateTo",
          publishDateTo);
    }

    // 커서 페이지 크기는 1이상의 정수
    if (limit < 1) {
      throw new InvalidParameterException("limit", limit);
    }

    CursorPageResponseArticleDto response = articleService.search(keyword, interestId,
        sourceIn, publishDateFrom, publishDateTo, orderBy, direction, cursor, after, limit,
        requestUserId);

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }
}
