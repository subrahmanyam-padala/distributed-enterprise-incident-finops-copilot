package com.opspilot.controlplane.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.controlplane.dto.alert.AlertWebhookRequest;
import com.opspilot.controlplane.dto.auth.RegisterRequest;
import com.opspilot.controlplane.repository.IncidentRepository;
import com.opspilot.controlplane.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Alert Webhook ingestion endpoint.
 * Verifies: 202 acceptance, alert deduplication, incident grouping,
 * severity escalation, outbox persistence, and input validation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AlertWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private UUID testOrgId;

    @BeforeEach
    void setUp() throws Exception {
        // Register a fresh organisation for each test to get a known org ID
        String slug = "alert-test-" + System.nanoTime();
        RegisterRequest reg = RegisterRequest.builder()
                .organizationName("Alert Test Org")
                .organizationSlug(slug)
                .adminEmail("alert-admin-" + System.nanoTime() + "@example.com")
                .adminPassword("AlertPass99")
                .adminFullName("Alert Admin")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String orgIdStr = objectMapper.readTree(body)
                .path("data").path("organization").path("id").asText();
        testOrgId = UUID.fromString(orgIdStr);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private AlertWebhookRequest buildAlert(String service, String severity, String env) {
        return AlertWebhookRequest.builder()
                .source("prometheus")
                .serviceName(service)
                .severity(severity)
                .message("CPU usage exceeded threshold on " + service)
                .environment(env)
                .organizationId(testOrgId)
                .build();
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void ingestAlert_validRequest_returns202AndCreatesIncident() throws Exception {
        long incidentsBefore = incidentRepository.count();

        AlertWebhookRequest alert = buildAlert("payment-service", "HIGH", "production");

        mockMvc.perform(post("/api/v1/alerts/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alert)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serviceName").value("payment-service"))
                .andExpect(jsonPath("$.data.severity").value("HIGH"))
                .andExpect(jsonPath("$.data.alertCount").value(1));

        assertThat(incidentRepository.count()).isEqualTo(incidentsBefore + 1);
    }

    @Test
    void ingestAlert_outboxEventPersisted() throws Exception {
        long outboxBefore = outboxEventRepository.count();

        AlertWebhookRequest alert = buildAlert("order-service", "CRITICAL", "production");

        mockMvc.perform(post("/api/v1/alerts/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alert)))
                .andExpect(status().isAccepted());

        // Outbox event must be written in the same transaction
        assertThat(outboxEventRepository.count()).isGreaterThan(outboxBefore);
    }

    @Test
    void ingestAlert_duplicateAlertSameFingerprint_groupsIntoSameIncident() throws Exception {
        // Supply an explicit fingerprint so the duplicate window triggers
        String fingerprint = "fp-dedup-" + System.nanoTime();

        AlertWebhookRequest first = AlertWebhookRequest.builder()
                .source("prometheus")
                .serviceName("inventory-service")
                .severity("MEDIUM")
                .message("Disk space low")
                .environment("production")
                .fingerprint(fingerprint)
                .organizationId(testOrgId)
                .build();

        AlertWebhookRequest second = AlertWebhookRequest.builder()
                .source("prometheus")
                .serviceName("inventory-service")
                .severity("MEDIUM")
                .message("Disk space low")
                .environment("production")
                .fingerprint(fingerprint)   // same fingerprint
                .organizationId(testOrgId)
                .build();

        // First alert creates incident
        MvcResult r1 = mockMvc.perform(post("/api/v1/alerts/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isAccepted())
                .andReturn();
        String id1 = objectMapper.readTree(r1.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Second alert is a duplicate; grouped into same incident
        MvcResult r2 = mockMvc.perform(post("/api/v1/alerts/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isAccepted())
                .andReturn();
        String id2 = objectMapper.readTree(r2.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Both must reference the same incident and alert count must be 2
        assertThat(id1).isEqualTo(id2);

        // Alert count incremented to 2
        mockMvc.perform(post("/api/v1/alerts/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.alertCount").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void ingestAlert_differentService_createsNewIncident() throws Exception {
        AlertWebhookRequest alertA = buildAlert("frontend-service", "HIGH", "production");
        AlertWebhookRequest alertB = buildAlert("analytics-service", "HIGH", "production");

        MvcResult r1 = mockMvc.perform(post("/api/v1/alerts/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alertA)))
                .andExpect(status().isAccepted())
                .andReturn();

        MvcResult r2 = mockMvc.perform(post("/api/v1/alerts/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alertB)))
                .andExpect(status().isAccepted())
                .andReturn();

        String id1 = objectMapper.readTree(r1.getResponse().getContentAsString())
                .path("data").path("id").asText();
        String id2 = objectMapper.readTree(r2.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Different services must produce different incidents
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void ingestAlert_missingSource_returns400() throws Exception {
        // source is @NotBlank — omit it
        String badJson = """
                {
                  "serviceName": "missing-source",
                  "severity": "HIGH",
                  "message": "Something broke",
                  "environment": "production",
                  "organizationId": "%s"
                }
                """.formatted(testOrgId);

        mockMvc.perform(post("/api/v1/alerts/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingestAlert_invalidSeverity_returns400() throws Exception {
        // The @Pattern constraint only allows CRITICAL|HIGH|MEDIUM|LOW|INFO
        String badJson = """
                {
                  "source": "prometheus",
                  "serviceName": "some-service",
                  "severity": "EXTREME",
                  "message": "Bad severity value",
                  "environment": "production",
                  "organizationId": "%s"
                }
                """.formatted(testOrgId);

        mockMvc.perform(post("/api/v1/alerts/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingestAlert_invalidEnvironment_returns400() throws Exception {
        String badJson = """
                {
                  "source": "prometheus",
                  "serviceName": "some-service",
                  "severity": "HIGH",
                  "message": "Bad environment",
                  "environment": "localhost",
                  "organizationId": "%s"
                }
                """.formatted(testOrgId);

        mockMvc.perform(post("/api/v1/alerts/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingestAlert_deterministicFingerprintForSameInput() throws Exception {
        // Two alerts with identical fields but no explicit fingerprint
        // must produce the same SHA-256 fingerprint, so they group together
        AlertWebhookRequest a = AlertWebhookRequest.builder()
                .source("datadog")
                .serviceName("shipping-service")
                .severity("CRITICAL")
                .message("5xx error rate spiking")
                .environment("production")
                .organizationId(testOrgId)
                .build();

        AlertWebhookRequest b = AlertWebhookRequest.builder()
                .source("datadog")
                .serviceName("shipping-service")
                .severity("CRITICAL")
                .message("5xx error rate spiking")
                .environment("production")
                .organizationId(testOrgId)
                .build();

        MvcResult r1 = mockMvc.perform(post("/api/v1/alerts/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(a)))
                .andExpect(status().isAccepted()).andReturn();

        MvcResult r2 = mockMvc.perform(post("/api/v1/alerts/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isAccepted()).andReturn();

        // Should resolve to the same incident (grouped by identical fingerprint)
        String id1 = objectMapper.readTree(r1.getResponse().getContentAsString())
                .path("data").path("id").asText();
        String id2 = objectMapper.readTree(r2.getResponse().getContentAsString())
                .path("data").path("id").asText();

        assertThat(id1).isEqualTo(id2);
    }

    // Needed for Hamcrest import
    private static org.hamcrest.Matcher<Integer> greaterThanOrEqualTo(int n) {
        return org.hamcrest.Matchers.greaterThanOrEqualTo(n);
    }
}
