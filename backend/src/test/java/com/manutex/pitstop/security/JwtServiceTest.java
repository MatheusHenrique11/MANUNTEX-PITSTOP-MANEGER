package com.manutex.pitstop.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private static final String VALID_SECRET =
        "test_secret_for_jwt_must_be_64_chars_minimum_for_hs512_algorithm!!";
    private static final long EXPIRY_MS = 900_000L;
    private static final String ISSUER = "pitstop.test";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(VALID_SECRET, EXPIRY_MS, ISSUER);
    }

    @Test
    void deveGerarTokenValido() {
        String token = jwtService.generateAccessToken("user@test.com", Map.of());
        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void deveExtrarirSubjectCorretamente() {
        String token = jwtService.generateAccessToken("user@test.com", Map.of());
        Claims claims = jwtService.validateAndExtract(token);
        assertThat(claims.getSubject()).isEqualTo("user@test.com");
    }

    @Test
    void deveIncluirClaimsExtras() {
        Map<String, Object> extra = Map.of("roles", List.of("ROLE_ADMIN"));
        String token = jwtService.generateAccessToken("admin@test.com", extra);
        Claims claims = jwtService.validateAndExtract(token);
        assertThat(claims.get("roles", List.class)).contains("ROLE_ADMIN");
    }

    @Test
    void deveGerarJtiUnicoPorToken() {
        String t1 = jwtService.generateAccessToken("user@test.com", Map.of());
        String t2 = jwtService.generateAccessToken("user@test.com", Map.of());
        Claims c1 = jwtService.validateAndExtract(t1);
        Claims c2 = jwtService.validateAndExtract(t2);
        assertThat(c1.getId()).isNotEqualTo(c2.getId());
    }

    @Test
    void deveRejeitarTokenTampering() {
        String token = jwtService.generateAccessToken("user@test.com", Map.of());
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void deveRejeitarTokenVazio() {
        assertThat(jwtService.isTokenValid("")).isFalse();
        assertThat(jwtService.isTokenValid("nao.e.um.jwt")).isFalse();
    }

    @Test
    void deveLancarExcecaoParaTokenInvalido() {
        assertThatThrownBy(() -> jwtService.validateAndExtract("token.invalido.aqui"))
            .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void deveFalharComSegredoCurto() {
        assertThatThrownBy(() -> new JwtService("curto_demais", EXPIRY_MS, ISSUER))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("64 caracteres");
    }
}
