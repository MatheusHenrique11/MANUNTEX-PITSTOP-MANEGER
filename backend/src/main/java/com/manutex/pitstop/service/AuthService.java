package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.RefreshToken;
import com.manutex.pitstop.domain.entity.User;
import com.manutex.pitstop.domain.repository.RefreshTokenRepository;
import com.manutex.pitstop.domain.repository.UserRepository;
import com.manutex.pitstop.security.JwtService;
import com.manutex.pitstop.web.dto.AuthRequest;
import com.manutex.pitstop.web.dto.AuthResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${app.jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthResponse login(AuthRequest request) {
        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (AuthenticationException e) {
            // Mensagem genérica — não revela se email ou senha está errado (OWASP)
            throw new InvalidCredentialsException("Credenciais inválidas");
        }

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        String accessToken = jwtService.generateAccessToken(
            user.getEmail(),
            Map.of("roles", user.getAuthorities().stream()
                .map(a -> a.getAuthority()).toList())
        );

        // Refresh token: gerado aleatoriamente, armazenado apenas como hash
        String rawRefreshToken = generateSecureToken();
        persistRefreshToken(user, rawRefreshToken);

        return new AuthResponse(
            accessToken,
            accessTokenExpiryMs / 1000,
            user.getRole().name(),
            user.getEmail()
        );
    }

    /**
     * Retorna o raw refresh token para ser injetado como cookie HTTP-Only pelo controller.
     * Nunca retorna no body da resposta.
     */
    @Transactional
    public String loginAndGetRefreshToken(AuthRequest request) {
        // Re-autentica para obter o token (o login() acima já validou)
        User user = userRepository.findByEmail(request.email())
            .orElseThrow();
        String rawRefreshToken = generateSecureToken();
        persistRefreshToken(user, rawRefreshToken);
        return rawRefreshToken;
    }

    @Transactional
    public AuthResponse refreshAccessToken(String rawRefreshToken) {
        String hash = hashToken(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> new InvalidCredentialsException("Refresh token inválido"));

        if (!stored.isValid()) {
            // Token expirado ou revogado — revoga todos do usuário (ataque de replay detectado)
            refreshTokenRepository.revokeAllByUserId(stored.getUser().getId());
            log.warn("Replay de refresh token detectado para user: {}", stored.getUser().getEmail());
            throw new InvalidCredentialsException("Refresh token expirado ou inválido");
        }

        // Rotação: revoga o token atual e emite um novo
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        String newRawToken = generateSecureToken();
        persistRefreshToken(user, newRawToken);

        String accessToken = jwtService.generateAccessToken(
            user.getEmail(),
            Map.of("roles", user.getAuthorities().stream()
                .map(a -> a.getAuthority()).toList())
        );

        return new AuthResponse(accessToken, accessTokenExpiryMs / 1000, user.getRole().name(), user.getEmail());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        String hash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void persistRefreshToken(User user, String rawToken) {
        RefreshToken rt = RefreshToken.builder()
            .tokenHash(hashToken(rawToken))
            .user(user)
            .expiresAt(Instant.now().plusMillis(refreshTokenExpiryMs))
            .build();
        refreshTokenRepository.save(rt);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[64];  // 512 bits de entropia
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) { super(message); }
    }
}
