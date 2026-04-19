package com.manutex.pitstop.web.dto;

/**
 * Retorna apenas o access token no body.
 * O refresh token é injetado como cookie HTTP-Only pelo AuthController,
 * nunca aparece no payload JSON.
 */
public record AuthResponse(
    String accessToken,
    long expiresIn,     // segundos até expiração do access token
    String role
) {}
