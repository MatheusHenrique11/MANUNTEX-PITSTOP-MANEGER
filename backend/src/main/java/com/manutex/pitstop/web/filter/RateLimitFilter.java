package com.manutex.pitstop.web.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting por IP usando Bucket4j com mapa em memória.
 * Cada IP tem seu próprio bucket com janela deslizante.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final long capacity;
    private final long refillTokens;
    private final long refillSeconds;

    public RateLimitFilter(
        @Value("${app.rate-limit.capacity:60}") long capacity,
        @Value("${app.rate-limit.refill-tokens:60}") long refillTokens,
        @Value("${app.rate-limit.refill-duration-seconds:60}") long refillSeconds
    ) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillSeconds = refillSeconds;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String key = resolveKey(request);
        Bucket bucket = buckets.computeIfAbsent(key, k -> buildBucket());

        long remaining = bucket.getAvailableTokens();
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit excedido para: {}", key);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Tente novamente em instantes.\"}");
        }
    }

    private Bucket buildBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(refillTokens, Duration.ofSeconds(refillSeconds))
                .build())
            .build();
    }

    private String resolveKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
