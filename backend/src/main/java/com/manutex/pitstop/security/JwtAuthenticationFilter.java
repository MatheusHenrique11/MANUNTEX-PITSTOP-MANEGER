package com.manutex.pitstop.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_COOKIE_NAME = "access_token";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        extractToken(request).ifPresent(token -> {
            try {
                Claims claims = jwtService.validateAndExtract(token);
                String subject = claims.getSubject();

                @SuppressWarnings("unchecked")
                List<String> roles = claims.get("roles", List.class);
                List<SimpleGrantedAuthority> authorities = roles == null
                    ? List.of()
                    : roles.stream().map(SimpleGrantedAuthority::new).toList();

                var auth = new UsernamePasswordAuthenticationToken(subject, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (InvalidTokenException e) {
                // Token inválido: SecurityContext permanece vazio → 401 automático
                log.debug("Rejeitando token inválido: {}", e.getMessage());
            }
        });

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        // 1. Tenta cookie HTTP-Only (preferido — imune a XSS)
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                .filter(c -> AUTH_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst();
        }
        // 2. Fallback: Authorization header (para clientes server-to-server)
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }
}
