package com.codeit.monew.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
    @Size(min = 1, max = 20)
    @NotBlank
    String nickname
) {

}
