package com.codeit.monew.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
    String password
) {

}
