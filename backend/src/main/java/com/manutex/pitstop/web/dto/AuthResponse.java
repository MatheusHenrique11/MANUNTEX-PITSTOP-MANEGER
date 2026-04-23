package com.manutex.pitstop.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
    String accessToken,
    long expiresIn,
    String role,
    String email,
    UUID empresaId
) {}
