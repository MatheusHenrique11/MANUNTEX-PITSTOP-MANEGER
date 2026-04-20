package com.manutex.pitstop.web.dto;

import com.manutex.pitstop.domain.enums.UserRole;
import jakarta.validation.constraints.*;

public record UserRequest(
    @NotBlank @Email @Size(max = 180)
    String email,

    @NotBlank @Size(min = 8, max = 72)
    String password,

    @NotBlank @Size(max = 150)
    String fullName,

    @NotNull
    UserRole role
) {}
