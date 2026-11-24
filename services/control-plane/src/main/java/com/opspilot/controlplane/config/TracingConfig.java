package com.opspilot.controlplane.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Distributed tracing configuration.
 * Enables Micrometer Observation + OpenTelemetry bridge for trace correlation.
 * Trace/span IDs are automatically propagated via MDC for structured logging.
 */
@Configuration
@ConditionalOnClass(ObservationRegistry.class)
public class TracingConfig {

    /**
     * Enables @Observed annotation for method-level tracing spans.
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}
