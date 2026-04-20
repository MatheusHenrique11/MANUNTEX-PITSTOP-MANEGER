package com.manutex.pitstop.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Retorna apenas o access token no body.
 * O refresh token é injetado como cookie HTTP-Only pelo AuthController,
 * nunca aparece no payload JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
    String accessToken,
    long expiresIn,
    String role,
    String email
) {}
