package com.opspilot.controlplane.dto.incident;

import com.opspilot.controlplane.domain.enums.IncidentSeverity;
import com.opspilot.controlplane.domain.enums.IncidentStatus;
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
public class IncidentResponse {
    private UUID id;
    private UUID organizationId;
    private String title;
    private IncidentSeverity severity;
    private IncidentStatus status;
    private String serviceName;
    private String environment;
    private Integer alertCount;
    private String summary;
    private Instant resolvedAt;
    private UUID acknowledgedBy;
    private Instant acknowledgedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
