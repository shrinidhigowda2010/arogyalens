package com.arogyalens.security;

import com.arogyalens.config.ArogyaLensProperties;
import com.arogyalens.dto.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory token bucket per client IP for AI-backed (POST /api/**) endpoints.
 * Protects the small Gemini quota from abuse. Returns 429 with a Retry-After header when exhausted.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final int capacity;
    private final double refillPerMilli;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitFilter(ArogyaLensProperties properties, ObjectMapper objectMapper) {
        this(properties.rateLimit().requestsPerMinute(), objectMapper, Clock.systemUTC());
    }

    RateLimitFilter(int requestsPerMinute, ObjectMapper objectMapper, Clock clock) {
        this.capacity = Math.max(0, requestsPerMinute);
        this.refillPerMilli = this.capacity / 60_000.0;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return capacity == 0
                || !"POST".equalsIgnoreCase(request.getMethod())
                || !request.getRequestURI().startsWith("/api/")
                || request.getRequestURI().startsWith("/api/safety/")
                || request.getRequestURI().startsWith("/api/history");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (tryConsume(request.getRemoteAddr())) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(429);
        response.setHeader("Retry-After", "60");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiError.of("RATE_LIMITED", "Too many requests",
                "You're sending requests too quickly. Please wait a minute and try again."));
    }

    boolean tryConsume(String clientId) {
        long now = clock.millis();
        if (buckets.size() > MAX_TRACKED_CLIENTS) {
            buckets.entrySet().removeIf(e -> now - e.getValue().updatedAt > 120_000);
        }
        Bucket bucket = buckets.computeIfAbsent(clientId == null ? "unknown" : clientId,
                k -> new Bucket(capacity, now));
        synchronized (bucket) {
            bucket.tokens = Math.min(capacity, bucket.tokens + (now - bucket.updatedAt) * refillPerMilli);
            bucket.updatedAt = now;
            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
                return true;
            }
            return false;
        }
    }

    private static final class Bucket {
        private double tokens;
        private long updatedAt;

        Bucket(double tokens, long updatedAt) {
            this.tokens = tokens;
            this.updatedAt = updatedAt;
        }
    }
}
