package com.manutex.pitstop.web.controller;

import com.manutex.pitstop.service.AuthService;
import com.manutex.pitstop.web.dto.AuthRequest;
import com.manutex.pitstop.web.dto.AuthResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";
    private static final String ACCESS_COOKIE  = "access_token";

    private final AuthService authService;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    @Value("${app.jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody AuthRequest request,
        HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(request);
        String rawRefreshToken = authService.loginAndGetRefreshToken(request);

        // Access token em cookie HTTP-Only (imune a XSS)
        addCookie(response, ACCESS_COOKIE, authResponse.accessToken(),
            (int) (accessTokenExpiryMs / 1000));

        // Refresh token em cookie HTTP-Only separado
        addCookie(response, REFRESH_COOKIE, rawRefreshToken,
            (int) (refreshTokenExpiryMs / 1000));

        // Body retorna apenas metadados (sem os tokens em texto)
        return ResponseEntity.ok(new AuthResponse(null, authResponse.expiresIn(), authResponse.role()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        String rawRefreshToken = extractCookie(request, REFRESH_COOKIE)
            .orElseThrow(() -> new AuthService.InvalidCredentialsException("Refresh token não encontrado"));

        AuthResponse authResponse = authService.refreshAccessToken(rawRefreshToken);

        // Renova os cookies com os novos tokens
        addCookie(response, ACCESS_COOKIE, authResponse.accessToken(),
            (int) (accessTokenExpiryMs / 1000));

        return ResponseEntity.ok(new AuthResponse(null, authResponse.expiresIn(), authResponse.role()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        String rawRefreshToken = extractCookie(request, REFRESH_COOKIE).orElse(null);
        authService.logout(rawRefreshToken);

        // Apaga os cookies no browser
        clearCookie(response, ACCESS_COOKIE);
        clearCookie(response, REFRESH_COOKIE);

        return ResponseEntity.noContent().build();
    }

    // ── Helpers de cookie ────────────────────────────────────────────────────

    private void addCookie(HttpServletResponse response, String name, String value, int maxAgeSec) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);       // só em HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSec);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void clearCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private Optional<String> extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
            .filter(c -> name.equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst();
    }
}
