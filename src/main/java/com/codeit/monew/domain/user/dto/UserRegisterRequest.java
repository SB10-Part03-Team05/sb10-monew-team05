package com.codeit.monew.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRegisterRequest(
    @Email
    @NotBlank
    String email,
    @Size(min = 1, max = 20)
    @NotBlank
    String nickname,
    @Size(min = 6, max = 20)
    @NotBlank
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{6,}$",
        message = "비밀번호는 영문, 숫자, 특수문자를 포함하여 6자 이상이어야 합니다."
    )
    String password
) {

}
