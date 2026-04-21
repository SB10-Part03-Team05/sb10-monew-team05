package com.codeit.monew.domain.article.dto.request;

import com.codeit.monew.domain.article.ArticleSource;
import com.codeit.monew.domain.article.entity.type.ArticleDirection;
import com.codeit.monew.domain.article.entity.type.ArticleOrderBy;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticleSearchRequest {

  @Parameter(description = "검색어(제목, 요약)")
  @Pattern(regexp = ".*\\S.*", message = "keyword는 공백으로 구성될 수 없습니다.")
  private String keyword;

  @Parameter(description = "관심사 ID")
  private UUID interestId;

  @Parameter(description = "출처(포함)")
  private List<ArticleSource> sourceIn;

  @Parameter(description = "날짜 시작(범위)")
  private Instant publishDateFrom;

  @Parameter(description = "날짜 끝(범위)")
  private Instant publishDateTo;

  @Parameter(description = "정렬 속성 이름", required = true)
  @NotNull(message = "정렬 속성은 필수입니다.")
  private ArticleOrderBy orderBy;

  @Parameter(description = "정렬 방향 (ASC, DESC)", required = true)
  @NotNull(message = "정렬 방향은 필수입니다.")
  private ArticleDirection direction;

  @Parameter(description = "커서 값")
  private String cursor;

  @Parameter(description = "보조 커서(createdAt) 값")
  private Instant after;

  @Parameter(description = "커서 페이지 크기", required = true)
  @NotNull(message = "커서 페이지 크기는 필수입니다.")
  @Min(value = 1, message = "limit은 1 이상이어야 한다.")
  private Integer limit;
}
