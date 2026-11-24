package com.opspilot.controlplane.dto.audit;

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
public class AuditLogResponse {
    private UUID id;
    private UUID organizationId;
    private UUID userId;
    private String action;
    private String entityType;
    private String entityId;
    private String details;
    private String ipAddress;
    private Instant createdAt;
}
