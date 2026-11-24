package com.opspilot.controlplane.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.controlplane.domain.entity.Incident;
import com.opspilot.controlplane.domain.entity.ProcessedEvent;
import com.opspilot.controlplane.domain.entity.TriageReport;
import com.opspilot.controlplane.domain.enums.IncidentStatus;
import com.opspilot.controlplane.repository.IncidentRepository;
import com.opspilot.controlplane.repository.ProcessedEventRepository;
import com.opspilot.controlplane.repository.TriageReportRepository;
import com.opspilot.controlplane.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Consumes incident.triage.completed.v1 events from Kafka emitted by Python AI Triage engine.
 * Ensures idempotent processing via processed_events table and updates incident status.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TriageCompletedConsumer {

    private final ObjectMapper objectMapper;
    private final TriageReportRepository triageReportRepository;
    private final IncidentRepository incidentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final AuditLogService auditLogService;

    @KafkaListener(
            topics = "${spring.kafka.topics.triage-completed:incident.triage.completed.v1}",
            groupId = "${spring.kafka.consumer.group-id:opspilot-control-plane}",
            autoStartup = "${opspilot.kafka.consumer.enabled:true}"
    )
    @Transactional
    public void onTriageCompleted(
            @Payload String payloadStr,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            Acknowledgment ack
    ) {
        log.info("Received triage completed event from topic [{}] key [{}]", topic, key);

        try {
            JsonNode envelope = objectMapper.readTree(payloadStr);
            String eventId = envelope.path("eventId").asText();
            String tenantIdStr = envelope.path("tenantId").asText();

            if (eventId.isBlank() || tenantIdStr.isBlank()) {
                log.error("Invalid event envelope: missing eventId or tenantId");
                if (ack != null) ack.acknowledge();
                return;
            }

            UUID organizationId = UUID.fromString(tenantIdStr);

            // 1. Idempotency Check
            if (processedEventRepository.existsByEventIdAndOrganizationId(eventId, organizationId)) {
                log.warn("Event [{}] already processed for tenant [{}]. Skipping duplicate.", eventId, organizationId);
                if (ack != null) ack.acknowledge();
                return;
            }

            JsonNode payload = envelope.path("payload");
            UUID incidentId = UUID.fromString(payload.path("incidentId").asText());
            UUID triageId = UUID.fromString(payload.path("triageId").asText());

            // 2. Persist TriageReport
            BigDecimal confidenceScore = payload.has("confidenceScore") && !payload.path("confidenceScore").isNull()
                    ? BigDecimal.valueOf(payload.path("confidenceScore").asDouble())
                    : null;
            Integer durationMs = payload.has("investigationDurationMs")
                    ? payload.path("investigationDurationMs").asInt()
                    : null;

            TriageReport report = TriageReport.builder()
                    .incidentId(incidentId)
                    .organizationId(organizationId)
                    .triageId(triageId)
                    .confidenceScore(confidenceScore)
                    .investigationDurationMs(durationMs)
                    .observations(payload.path("observations").toString())
                    .supportingEvidence(payload.path("supportingEvidence").toString())
                    .potentialRootCauses(payload.path("potentialRootCauses").toString())
                    .suggestedRemediations(payload.path("suggestedRemediations").toString())
                    .runbookCitations(payload.path("runbookCitations").toString())
                    .uncertainties(payload.path("uncertainties").toString())
                    .additionalSteps(payload.path("additionalInvestigationSteps").toString())
                    .build();

            triageReportRepository.save(report);

            // 3. Update Incident status if OPEN -> INVESTIGATING
            Optional<Incident> optIncident = incidentRepository.findById(incidentId);
            if (optIncident.isPresent()) {
                Incident inc = optIncident.get();
                if (inc.getStatus() == IncidentStatus.OPEN) {
                    inc.setStatus(IncidentStatus.INVESTIGATING);
                    incidentRepository.save(inc);
                }
            }

            // 4. Mark Event as Processed (Idempotency key)
            ProcessedEvent processed = ProcessedEvent.builder()
                    .eventId(eventId)
                    .organizationId(organizationId)
                    .topic(topic)
                    .build();
            processedEventRepository.save(processed);

            // 5. Audit Log
            auditLogService.record(
                    organizationId,
                    null,
                    "TRIAGE_REPORT_PERSISTED",
                    "TriageReport",
                    report.getId().toString(),
                    "{\"incidentId\":\"" + incidentId + "\",\"triageId\":\"" + triageId + "\"}",
                    null,
                    null
            );

            log.info("Successfully processed and saved triage report [{}] for incident [{}]",
                    report.getId(), incidentId);

            if (ack != null) {
                ack.acknowledge();
            }
        } catch (Exception ex) {
            log.error("Error processing triage completed event: {}", ex.getMessage(), ex);
            // In a production context with transactional listener, rethrow or send to DLQ
            throw new RuntimeException("Failed to process triage completed event", ex);
        }
    }
}
