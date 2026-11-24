package com.opspilot.controlplane.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.controlplane.domain.entity.Alert;
import com.opspilot.controlplane.domain.entity.Incident;
import com.opspilot.controlplane.domain.entity.OutboxEvent;
import com.opspilot.controlplane.domain.enums.IncidentSeverity;
import com.opspilot.controlplane.domain.enums.IncidentStatus;
import com.opspilot.controlplane.domain.enums.OutboxStatus;
import com.opspilot.controlplane.dto.alert.AlertWebhookRequest;
import com.opspilot.controlplane.dto.incident.IncidentResponse;
import com.opspilot.controlplane.exception.ValidationException;
import com.opspilot.controlplane.repository.AlertRepository;
import com.opspilot.controlplane.repository.IncidentRepository;
import com.opspilot.controlplane.repository.OrganizationRepository;
import com.opspilot.controlplane.repository.OutboxEventRepository;
import com.opspilot.controlplane.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertIngestionService {

    private final IncidentRepository incidentRepository;
    private final AlertRepository alertRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    private static final String TOPIC_TRIAGE_REQUESTED = "incident.triage.requested.v1";
    private static final List<IncidentStatus> ACTIVE_STATUSES = List.of(
            IncidentStatus.OPEN,
            IncidentStatus.INVESTIGATING,
            IncidentStatus.IDENTIFIED,
            IncidentStatus.MONITORING
    );

    @Transactional
    public IncidentResponse ingestAlert(AlertWebhookRequest request) {
        UUID organizationId = resolveOrganizationId(request);

        // 1. Compute deterministic SHA-256 fingerprint if not supplied
        String fingerprint = StringUtils.hasText(request.getFingerprint())
                ? request.getFingerprint()
                : computeFingerprint(request);

        // 2. Check for duplicate alert within sliding 15-minute window
        Instant fifteenMinutesAgo = Instant.now().minus(15, ChronoUnit.MINUTES);
        Optional<Alert> duplicateAlert = alertRepository
                .findFirstByOrganizationIdAndFingerprintAndReceivedAtAfterOrderByReceivedAtDesc(
                        organizationId, fingerprint, fifteenMinutesAgo
                );

        Incident incident;
        boolean isNewIncident = false;

        // 3. Find active incident for the same service + environment in the last 1 hour
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        Optional<Incident> existingIncident = incidentRepository
                .findFirstByOrganizationIdAndServiceNameAndEnvironmentAndStatusInAndCreatedAtAfterOrderByCreatedAtDesc(
                        organizationId,
                        request.getServiceName(),
                        request.getEnvironment(),
                        ACTIVE_STATUSES,
                        oneHourAgo
                );

        IncidentSeverity alertSeverity = IncidentSeverity.valueOf(request.getSeverity());

        if (existingIncident.isPresent()) {
            incident = existingIncident.get();
            incident.setAlertCount(incident.getAlertCount() + 1);

            // Escalate severity if new alert is more severe
            if (isHigherSeverity(alertSeverity, incident.getSeverity())) {
                incident.setSeverity(alertSeverity);
            }
            incident = incidentRepository.save(incident);
            log.info("Grouped alert into existing incident [{}] for service [{}] (count={})",
                    incident.getId(), request.getServiceName(), incident.getAlertCount());
        } else {
            isNewIncident = true;
            incident = Incident.builder()
                    .organizationId(organizationId)
                    .title(generateIncidentTitle(request))
                    .severity(alertSeverity)
                    .status(IncidentStatus.OPEN)
                    .serviceName(request.getServiceName())
                    .environment(request.getEnvironment())
                    .alertCount(1)
                    .summary(request.getMessage())
                    .build();
            incident = incidentRepository.save(incident);
            log.info("Created new incident [{}] for service [{}] with severity [{}]",
                    incident.getId(), request.getServiceName(), alertSeverity);
        }

        // 4. Persist Alert entity linked to Incident
        String labelsJson = serializeJson(request.getLabels() != null ? request.getLabels() : Map.of());
        String metadataJson = serializeJson(request.getMetadata() != null ? request.getMetadata() : Map.of());

        Alert alert = Alert.builder()
                .incident(incident)
                .organizationId(organizationId)
                .externalAlertId(request.getAlertId())
                .source(request.getSource())
                .serviceName(request.getServiceName())
                .severity(request.getSeverity())
                .message(request.getMessage())
                .environment(request.getEnvironment())
                .fingerprint(fingerprint)
                .labels(labelsJson)
                .metadata(metadataJson)
                .build();
        alertRepository.save(alert);

        // 5. Transactional Outbox Pattern: Save outbox event in the SAME transaction
        publishToOutbox(incident, alert, organizationId);

        // 6. Record Audit Log
        auditLogService.record(
                organizationId,
                null,
                isNewIncident ? "INCIDENT_CREATED" : "INCIDENT_ALERT_GROUPED",
                "Incident",
                incident.getId().toString(),
                "{\"alertId\":\"" + alert.getId() + "\",\"severity\":\"" + alert.getSeverity() + "\"}",
                null,
                null
        );

        return mapToIncidentResponse(incident);
    }

    private UUID resolveOrganizationId(AlertWebhookRequest request) {
        // Derive tenant from authenticated context first, then request payload
        UUID contextOrgId = TenantContext.getOrganizationId();
        if (contextOrgId != null) {
            return contextOrgId;
        }

        if (request.getOrganizationId() != null) {
            if (!organizationRepository.existsById(request.getOrganizationId())) {
                throw new ValidationException("Specified organization does not exist");
            }
            return request.getOrganizationId();
        }

        throw new ValidationException("Tenant context required: provide authenticated token or valid organizationId");
    }

    private String computeFingerprint(AlertWebhookRequest req) {
        String raw = String.format("%s:%s:%s:%s:%s",
                req.getServiceName(), req.getSeverity(), req.getEnvironment(), req.getSource(), req.getMessage());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    private void publishToOutbox(Incident incident, Alert alert, UUID organizationId) {
        String eventId = UUID.randomUUID().toString();
        String correlationId = "corr-" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", TOPIC_TRIAGE_REQUESTED);
        envelope.put("schemaVersion", "1.0.0");
        envelope.put("tenantId", organizationId.toString());
        envelope.put("timestamp", Instant.now().toString());
        envelope.put("correlationId", correlationId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("incidentId", incident.getId().toString());
        payload.put("title", incident.getTitle());
        payload.put("severity", incident.getSeverity().name());
        payload.put("serviceName", incident.getServiceName());
        payload.put("environment", incident.getEnvironment());
        payload.put("status", incident.getStatus().name());
        payload.put("alertCount", incident.getAlertCount());
        payload.put("initialAlertSummary", incident.getSummary());
        payload.put("fingerprints", List.of(alert.getFingerprint()));

        envelope.put("payload", payload);

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventId(eventId)
                .organizationId(organizationId)
                .topic(TOPIC_TRIAGE_REQUESTED)
                .partitionKey(incident.getServiceName())
                .status(OutboxStatus.PENDING)
                .payload(serializeJson(envelope))
                .build();

        outboxEventRepository.save(outboxEvent);
        log.info("Saved outbox event [{}] for incident [{}] topic [{}]",
                eventId, incident.getId(), TOPIC_TRIAGE_REQUESTED);
    }

    private boolean isHigherSeverity(IncidentSeverity a, IncidentSeverity b) {
        int priorityA = getSeverityRank(a);
        int priorityB = getSeverityRank(b);
        return priorityA < priorityB; // lower rank number = higher severity
    }

    private int getSeverityRank(IncidentSeverity s) {
        return switch (s) {
            case CRITICAL -> 1;
            case HIGH -> 2;
            case MEDIUM -> 3;
            case LOW -> 4;
            case INFO -> 5;
        };
    }

    private String generateIncidentTitle(AlertWebhookRequest req) {
        return String.format("[%s] %s alert on %s (%s)",
                req.getSeverity(), req.getSource(), req.getServiceName(), req.getEnvironment());
    }

    private String serializeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    private IncidentResponse mapToIncidentResponse(Incident inc) {
        return IncidentResponse.builder()
                .id(inc.getId())
                .organizationId(inc.getOrganizationId())
                .title(inc.getTitle())
                .severity(inc.getSeverity())
                .status(inc.getStatus())
                .serviceName(inc.getServiceName())
                .environment(inc.getEnvironment())
                .alertCount(inc.getAlertCount())
                .summary(inc.getSummary())
                .resolvedAt(inc.getResolvedAt())
                .acknowledgedBy(inc.getAcknowledgedBy())
                .acknowledgedAt(inc.getAcknowledgedAt())
                .createdAt(inc.getCreatedAt())
                .updatedAt(inc.getUpdatedAt())
                .build();
    }
}
