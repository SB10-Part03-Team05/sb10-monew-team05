package com.codeit.monew.domain.interest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "수정할 관심사 정보")
public record InterestUpdateRequest(
    @Schema(description = "수정 키워드 목록", maxLength = 10)
    @NotEmpty
    @Size(max = 10)
    List<@NotBlank String> keywords
) {}
