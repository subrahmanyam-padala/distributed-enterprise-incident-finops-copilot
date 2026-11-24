package com.opspilot.controlplane.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Injects W3C-compatible trace correlation IDs into MDC and response headers.
 * If an incoming request carries a traceparent header, that trace ID is reused.
 * Otherwise a new correlation ID is generated.
 *
 * This ensures every log line in a request includes a correlationId for
 * distributed tracing across the control-plane, AI triage engine, and MCP server.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceCorrelationFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String TRACEPARENT_HEADER = "traceparent";
    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_REQUEST_METHOD = "requestMethod";
    private static final String MDC_REQUEST_PATH = "requestPath";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = extractOrGenerateCorrelationId(request);

        MDC.put(MDC_CORRELATION_ID, correlationId);
        MDC.put(MDC_REQUEST_METHOD, request.getMethod());
        MDC.put(MDC_REQUEST_PATH, request.getRequestURI());

        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            log.debug("Request started: {} {} [correlationId={}]",
                    request.getMethod(), request.getRequestURI(), correlationId);
            filterChain.doFilter(request, response);
        } finally {
            log.debug("Request completed: {} {} [status={}, correlationId={}]",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), correlationId);
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_REQUEST_METHOD);
            MDC.remove(MDC_REQUEST_PATH);
        }
    }

    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        // 1. Check for explicit correlation ID header
        String existing = request.getHeader(CORRELATION_ID_HEADER);
        if (existing != null && !existing.isBlank()) {
            return existing;
        }

        // 2. Extract from W3C traceparent header (format: version-traceId-spanId-flags)
        String traceparent = request.getHeader(TRACEPARENT_HEADER);
        if (traceparent != null) {
            String[] parts = traceparent.split("-");
            if (parts.length >= 2) {
                return parts[1]; // trace ID
            }
        }

        // 3. Generate new UUID-based correlation ID
        return UUID.randomUUID().toString();
    }
}
