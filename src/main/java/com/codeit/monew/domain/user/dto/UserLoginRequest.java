package com.codeit.monew.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "로그인 정보")
public record UserLoginRequest(
    @Schema(description = "로그인 이메일")
    @Email
    @NotBlank
    String email,
    @Schema(description = "로그인 비밀번호")
    @Size(min = 6, max = 20)
    @NotBlank
    String password
) {

}
