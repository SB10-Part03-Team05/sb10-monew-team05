package com.codeit.monew.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "수정할 사용자 정보")
public record UserUpdateRequest(
    @Schema(description = "수정 닉네임")
    @Size(min = 1, max = 20)
    @NotBlank
    String nickname
) {

}
