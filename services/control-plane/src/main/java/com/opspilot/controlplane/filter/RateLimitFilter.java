package com.opspilot.controlplane.filter;

import com.opspilot.controlplane.config.RateLimitingConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis-backed sliding window rate limiter.
 * Uses per-tenant (or per-IP) counters in Redis with configurable windows.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(RateLimitingConfig.class)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RateLimitingConfig config;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!config.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip rate limiting for health/actuator endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/actuator") || path.equals("/api/v1/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = resolveClientKey(request);
        String redisKey = "rate_limit:" + clientKey;

        try {
            Long currentCount = redisTemplate.opsForValue().increment(redisKey);
            if (currentCount != null && currentCount == 1L) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(config.getWindowSeconds()));
            }

            int remaining = Math.max(0, config.getMaxRequests() - (currentCount != null ? currentCount.intValue() : 0));

            response.setHeader(config.getLimitHeader(), String.valueOf(config.getMaxRequests()));
            response.setHeader(config.getRemainingHeader(), String.valueOf(remaining));

            if (currentCount != null && currentCount > config.getMaxRequests()) {
                Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
                response.setHeader(config.getRetryAfterHeader(), String.valueOf(ttl != null ? ttl : config.getWindowSeconds()));
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"status\":\"error\",\"message\":\"Rate limit exceeded. Try again in " +
                                (ttl != null ? ttl : config.getWindowSeconds()) + " seconds.\",\"code\":429}"
                );
                log.warn("Rate limit exceeded for client [{}], count={}", clientKey, currentCount);
                return;
            }
        } catch (Exception e) {
            // If Redis is down, fail open — allow the request through
            log.warn("Rate limiting unavailable (Redis error), failing open: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Resolve rate limit key: prefer tenant org ID from JWT, fallback to IP.
     */
    private String resolveClientKey(HttpServletRequest request) {
        // Try to extract org ID from JWT auth header (already parsed by security filter)
        String orgHeader = request.getHeader("X-Organization-Id");
        if (orgHeader != null && !orgHeader.isBlank()) {
            return "org:" + orgHeader;
        }

        // Check if principal has an org ID (from SecurityContext)
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            return "user:" + auth.getName();
        }

        // Fallback to client IP
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
        return "ip:" + ip;
    }
}
