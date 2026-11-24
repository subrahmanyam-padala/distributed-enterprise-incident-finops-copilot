package com.opspilot.controlplane.dto.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private UUID id;
    private UUID incidentId;
    private UUID organizationId;
    private String externalAlertId;
    private String source;
    private String serviceName;
    private String severity;
    private String message;
    private String environment;
    private String fingerprint;
    private Instant receivedAt;
}
