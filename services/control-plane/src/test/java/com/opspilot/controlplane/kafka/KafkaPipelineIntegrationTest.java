package com.opspilot.controlplane.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.controlplane.domain.entity.Incident;
import com.opspilot.controlplane.domain.entity.Organization;
import com.opspilot.controlplane.domain.entity.OutboxEvent;
import com.opspilot.controlplane.domain.entity.TriageReport;
import com.opspilot.controlplane.domain.enums.IncidentSeverity;
import com.opspilot.controlplane.domain.enums.IncidentStatus;
import com.opspilot.controlplane.domain.enums.OutboxStatus;
import com.opspilot.controlplane.repository.IncidentRepository;
import com.opspilot.controlplane.repository.OrganizationRepository;
import com.opspilot.controlplane.repository.OutboxEventRepository;
import com.opspilot.controlplane.repository.TriageReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit and integration tests for Kafka Outbox publishing and Triage completion consumption.
 */
@SpringBootTest
@ActiveProfiles("test")
class KafkaPipelineIntegrationTest {

    @Autowired
    private ScheduledOutboxPublisher outboxPublisher;

    @Autowired
    private TriageCompletedConsumer triageCompletedConsumer;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private TriageReportRepository triageReportRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KafkaEventProducer kafkaEventProducer;

    private UUID orgId;
    private Incident incident;

    @BeforeEach
    void setUp() {
        Organization org = Organization.builder()
                .name("Kafka Test Org")
                .slug("kafka-test-" + System.nanoTime())
                .plan("enterprise")
                .active(true)
                .build();
        org = organizationRepository.save(org);
        orgId = org.getId();

        incident = Incident.builder()
                .organizationId(orgId)
                .title("Kafka Pipeline Incident")
                .severity(IncidentSeverity.HIGH)
                .status(IncidentStatus.OPEN)
                .serviceName("checkout-service")
                .environment("production")
                .alertCount(1)
                .summary("Payment failure rate spike")
                .build();
        incident = incidentRepository.save(incident);
    }

    @Test
    void outboxPublisher_publishesPendingEventsAndMarksStatusPublished() {
        String eventId = UUID.randomUUID().toString();
        OutboxEvent event = OutboxEvent.builder()
                .eventId(eventId)
                .organizationId(orgId)
                .topic("incident.triage.requested.v1")
                .partitionKey("checkout-service")
                .status(OutboxStatus.PENDING)
                .payload("{\"test\": true}")
                .build();
        event = outboxEventRepository.save(event);

        @SuppressWarnings("unchecked")
        SendResult<String, String> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaEventProducer.send(anyString(), anyString(), anyString())).thenReturn(future);

        outboxPublisher.publishSingleEvent(event);

        OutboxEvent updated = outboxEventRepository.findByEventId(eventId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(updated.getPublishedAt()).isNotNull();
        assertThat(updated.getLastError()).isNull();
    }

    @Test
    void triageCompletedConsumer_persistsReportAndUpdatesIncidentStatusIdempotently() throws Exception {
        String eventId = "evt-triage-comp-" + System.nanoTime();
        UUID triageId = UUID.randomUUID();

        String payload = """
                {
                  "eventId": "%s",
                  "eventType": "incident.triage.completed.v1",
                  "schemaVersion": "1.0.0",
                  "tenantId": "%s",
                  "timestamp": "2026-11-24T06:00:00Z",
                  "correlationId": "corr-test-123",
                  "payload": {
                    "incidentId": "%s",
                    "triageId": "%s",
                    "investigationDurationMs": 1420,
                    "confidenceScore": 0.88,
                    "observations": ["Database connection pool exhaustion on replica 2"],
                    "supportingEvidence": [
                      {
                        "source": "datadog",
                        "evidenceType": "METRIC",
                        "description": "Active connections: 100/100"
                      }
                    ],
                    "potentialRootCauses": [
                      {
                        "hypothesis": "Unindexed full table scan during batch export",
                        "likelihood": "HIGH",
                        "reasoning": "Correlates with cron execution at 05:58"
                      }
                    ],
                    "suggestedRemediations": [
                      {
                        "actionTitle": "Kill long running batch query",
                        "actionDescription": "SELECT * FROM orders WHERE status='PENDING'",
                        "riskLevel": "MEDIUM",
                        "requiresApproval": true
                      }
                    ],
                    "runbookCitations": [
                      {
                        "documentId": "rb-db-pool",
                        "title": "Postgres Pool Exhaustion Runbook",
                        "chunkId": "chunk-1",
                        "relevanceScore": 0.94
                      }
                    ],
                    "uncertainties": [],
                    "additionalInvestigationSteps": ["Inspect pg_stat_activity"],
                    "completedAt": "2026-11-24T06:01:00Z"
                  }
                }
                """.formatted(eventId, orgId, incident.getId(), triageId);

        // 1. First consumption: must persist report and move incident to INVESTIGATING
        triageCompletedConsumer.onTriageCompleted(payload, "incident.triage.completed.v1", "checkout-service", null);

        List<TriageReport> reports = triageReportRepository.findByIncidentIdOrderByCreatedAtDesc(incident.getId());
        assertThat(reports).hasSize(1);
        TriageReport report = reports.get(0);
        assertThat(report.getTriageId()).isEqualTo(triageId);
        assertThat(report.getInvestigationDurationMs()).isEqualTo(1420);
        assertThat(report.getConfidenceScore().doubleValue()).isEqualTo(0.880);

        Incident updatedIncident = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(updatedIncident.getStatus()).isEqualTo(IncidentStatus.INVESTIGATING);

        // 2. Idempotency test: consuming same eventId must not duplicate report
        triageCompletedConsumer.onTriageCompleted(payload, "incident.triage.completed.v1", "checkout-service", null);

        List<TriageReport> reportsAfterDuplicate = triageReportRepository.findByIncidentIdOrderByCreatedAtDesc(incident.getId());
        assertThat(reportsAfterDuplicate).hasSize(1);
    }
}
