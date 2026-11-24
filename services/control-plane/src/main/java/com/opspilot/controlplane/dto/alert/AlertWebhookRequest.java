package com.opspilot.controlplane.dto.alert;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertWebhookRequest {

    private String alertId;

    @NotBlank(message = "Source is required (e.g. Prometheus, Datadog)")
    private String source;

    @NotBlank(message = "Service name is required")
    private String serviceName;

    @NotBlank(message = "Severity is required")
    @Pattern(regexp = "^(CRITICAL|HIGH|MEDIUM|LOW|INFO)$", message = "Severity must be CRITICAL, HIGH, MEDIUM, LOW, or INFO")
    private String severity;

    @NotBlank(message = "Alert message is required")
    private String message;

    @NotBlank(message = "Environment is required")
    @Pattern(regexp = "^(production|staging|development|qa)$", message = "Environment must be production, staging, development, or qa")
    private String environment;

    private String fingerprint;

    private Map<String, String> labels;

    private Map<String, Object> metadata;

    private UUID organizationId;
}
