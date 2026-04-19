package com.manutex.pitstop.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpiryMs;
    private final String issuer;

    public JwtService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.access-token-expiry-ms}") long accessTokenExpiryMs,
        @Value("${app.jwt.issuer}") String issuer
    ) {
        // Garante chave mínima de 512 bits para HS512
        if (secret.length() < 64) {
            throw new IllegalStateException("JWT secret deve ter ao menos 64 caracteres (512 bits)");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.issuer = issuer;
    }

    public String generateAccessToken(String subject, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        return Jwts.builder()
            .id(UUID.randomUUID().toString())   // jti — previne replay attacks
            .issuer(issuer)
            .subject(subject)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(accessTokenExpiryMs)))
            .claims(extraClaims)
            .signWith(signingKey, Jwts.SIG.HS512)
            .compact();
    }

    public Claims validateAndExtract(String token) {
        try {
            return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            throw new InvalidTokenException("Token inválido ou expirado");
        }
    }

    public boolean isTokenValid(String token) {
        try {
            validateAndExtract(token);
            return true;
        } catch (InvalidTokenException e) {
            return false;
        }
    }
}
