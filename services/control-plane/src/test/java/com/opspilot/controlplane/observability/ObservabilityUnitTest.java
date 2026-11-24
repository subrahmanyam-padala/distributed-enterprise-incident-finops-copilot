package com.opspilot.controlplane.observability;

import com.opspilot.controlplane.config.RateLimitingConfig;
import com.opspilot.controlplane.filter.RateLimitFilter;
import com.opspilot.controlplane.filter.TraceCorrelationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityUnitTest {

    @Test
    @DisplayName("TraceCorrelationFilter injects X-Correlation-Id header in response")
    void traceCorrelationFilter_injectsCorrelationHeader() throws Exception {
        TraceCorrelationFilter filter = new TraceCorrelationFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        String correlationId = response.getHeader("X-Correlation-Id");
        assertThat(correlationId).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("TraceCorrelationFilter reuses existing X-Correlation-Id from request")
    void traceCorrelationFilter_reusesExistingCorrelationId() throws Exception {
        TraceCorrelationFilter filter = new TraceCorrelationFilter();
        String existingId = "test-correlation-12345";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        request.addHeader("X-Correlation-Id", existingId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo(existingId);
    }

    @Test
    @DisplayName("TraceCorrelationFilter extracts trace ID from W3C traceparent header")
    void traceCorrelationFilter_extractsW3CTraceId() throws Exception {
        TraceCorrelationFilter filter = new TraceCorrelationFilter();
        String traceId = "0af7651916cd43dd8448eb211c80319c";
        String traceparent = "00-" + traceId + "-b7ad6b7169203331-01";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        request.addHeader("traceparent", traceparent);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo(traceId);
    }

    @Test
    @DisplayName("RateLimitingConfig has expected default properties")
    void rateLimitingConfig_defaults() {
        RateLimitingConfig config = new RateLimitingConfig();
        assertThat(config.isEnabled()).isTrue();
        assertThat(config.getMaxRequests()).isEqualTo(100);
        assertThat(config.getWindowSeconds()).isEqualTo(60);
        assertThat(config.getRemainingHeader()).isEqualTo("X-RateLimit-Remaining");
        assertThat(config.getLimitHeader()).isEqualTo("X-RateLimit-Limit");
    }

    @Test
    @DisplayName("IncidentService has @Cacheable annotations for caching support")
    void incidentService_hasCacheableAnnotations() throws Exception {
        var serviceClass = Class.forName("com.opspilot.controlplane.service.IncidentService");

        var getByIdMethod = serviceClass.getMethod("getIncidentById", java.util.UUID.class);
        assertThat(getByIdMethod.isAnnotationPresent(org.springframework.cache.annotation.Cacheable.class)).isTrue();

        var getTriageMethod = serviceClass.getMethod("getTriageReportsForIncident", java.util.UUID.class);
        assertThat(getTriageMethod.isAnnotationPresent(org.springframework.cache.annotation.Cacheable.class)).isTrue();

        var acknowledgeMethod = serviceClass.getMethod("acknowledgeIncident", java.util.UUID.class,
                com.opspilot.controlplane.dto.incident.IncidentAcknowledgeRequest.class);
        assertThat(acknowledgeMethod.isAnnotationPresent(org.springframework.cache.annotation.CacheEvict.class)).isTrue();
    }
}
