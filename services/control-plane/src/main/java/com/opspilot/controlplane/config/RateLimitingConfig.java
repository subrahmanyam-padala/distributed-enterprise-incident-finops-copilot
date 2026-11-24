package com.opspilot.controlplane.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Rate limiting configuration properties.
 * Configurable per-tenant sliding window rate limiter backed by Redis.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "opspilot.rate-limit")
@ConditionalOnProperty(name = "opspilot.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitingConfig {

    /** Whether rate limiting is enabled */
    private boolean enabled = true;

    /** Maximum requests per window */
    private int maxRequests = 100;

    /** Window size in seconds */
    private int windowSeconds = 60;

    /** Header to return remaining request count */
    private String remainingHeader = "X-RateLimit-Remaining";

    /** Header to return the limit */
    private String limitHeader = "X-RateLimit-Limit";

    /** Header to return retry-after seconds when throttled */
    private String retryAfterHeader = "Retry-After";
}
