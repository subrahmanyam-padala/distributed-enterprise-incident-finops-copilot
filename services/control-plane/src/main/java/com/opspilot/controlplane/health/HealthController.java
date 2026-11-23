package com.opspilot.controlplane.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Simple public health check endpoint.
 * Spring Actuator at /actuator/health provides the full health status.
 * This endpoint provides a quick unauthenticated liveness probe.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "opspilot-control-plane",
                "timestamp", Instant.now().toString(),
                "version", "0.1.0-SNAPSHOT"
        );
    }
}
