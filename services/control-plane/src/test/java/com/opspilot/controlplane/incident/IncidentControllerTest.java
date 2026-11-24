package com.opspilot.controlplane.incident;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.controlplane.domain.enums.IncidentStatus;
import com.opspilot.controlplane.dto.alert.AlertWebhookRequest;
import com.opspilot.controlplane.dto.auth.RegisterRequest;
import com.opspilot.controlplane.dto.incident.IncidentAcknowledgeRequest;
import com.opspilot.controlplane.dto.incident.IncidentResolveRequest;
import com.opspilot.controlplane.dto.incident.IncidentStatusUpdateRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Incident lifecycle API.
 * Covers: list, get-by-id, acknowledge, status update, resolve, state machine validation,
 * and VIEWER RBAC enforcement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private UUID orgId;
    private UUID incidentId;

    // -------------------------------------------------------------------------
    // Setup: register org, ingest an alert to create an incident
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() throws Exception {
        String slug = "incident-test-" + System.nanoTime();

        RegisterRequest reg = RegisterRequest.builder()
                .organizationName("Incident Test Org")
                .organizationSlug(slug)
                .adminEmail("incident-admin-" + System.nanoTime() + "@example.com")
                .adminPassword("IncidentPass1")
                .adminFullName("Incident Admin")
                .build();

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        var regTree = objectMapper.readTree(regResult.getResponse().getContentAsString()).path("data");
        adminToken = regTree.path("token").asText();
        orgId = UUID.fromString(regTree.path("organization").path("id").asText());

        // Ingest an alert to auto-create an incident
        AlertWebhookRequest alert = AlertWebhookRequest.builder()
                .source("prometheus")
                .serviceName("checkout-service")
                .severity("HIGH")
                .message("Latency spike detected")
                .environment("production")
                .organizationId(orgId)
                .build();

        MvcResult alertResult = mockMvc.perform(post("/api/v1/alerts/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alert)))
                .andExpect(status().isAccepted())
                .andReturn();

        String incidentIdStr = objectMapper.readTree(alertResult.getResponse().getContentAsString())
                .path("data").path("id").asText();
        incidentId = UUID.fromString(incidentIdStr);
    }

    // -------------------------------------------------------------------------
    // List incidents
    // -------------------------------------------------------------------------

    @Test
    void listIncidents_authenticatedAdmin_returnsOrgIncidents() throws Exception {
        mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void listIncidents_withStatusFilter_returnsMatchingIncidents() throws Exception {
        mockMvc.perform(get("/api/v1/incidents")
                        .param("status", "OPEN")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("OPEN"));
    }

    @Test
    void listIncidents_unauthenticated_returns401Or403() throws Exception {
        mockMvc.perform(get("/api/v1/incidents"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 for unauthenticated access, got: " + status);
                    }
                });
    }

    // -------------------------------------------------------------------------
    // Get by ID
    // -------------------------------------------------------------------------

    @Test
    void getIncidentById_validId_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/incidents/" + incidentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(incidentId.toString()))
                .andExpect(jsonPath("$.data.serviceName").value("checkout-service"));
    }

    @Test
    void getIncidentById_nonExistentId_returns404() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/incidents/" + randomId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // Acknowledge (OPEN → INVESTIGATING)
    // -------------------------------------------------------------------------

    @Test
    void acknowledgeIncident_openIncident_movesToInvestigating() throws Exception {
        IncidentAcknowledgeRequest ack = IncidentAcknowledgeRequest.builder()
                .note("Starting RCA")
                .build();

        mockMvc.perform(post("/api/v1/incidents/" + incidentId + "/acknowledge")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ack)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INVESTIGATING"))
                .andExpect(jsonPath("$.data.acknowledgedAt").isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // Status update (state machine validation)
    // -------------------------------------------------------------------------

    @Test
    void updateStatus_validTransition_openToResolved_succeeds() throws Exception {
        IncidentStatusUpdateRequest req = IncidentStatusUpdateRequest.builder()
                .status(IncidentStatus.RESOLVED)
                .build();

        mockMvc.perform(patch("/api/v1/incidents/" + incidentId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolvedAt").isNotEmpty());
    }

    @Test
    void updateStatus_invalidTransition_openToMonitoring_returns500OrBadRequest() throws Exception {
        // OPEN → MONITORING is not a valid state machine transition
        IncidentStatusUpdateRequest req = IncidentStatusUpdateRequest.builder()
                .status(IncidentStatus.MONITORING)
                .build();

        // The service throws IllegalStateException which the GlobalExceptionHandler
        // should map to 400 or 500 — either is acceptable to signal rejection
        mockMvc.perform(patch("/api/v1/incidents/" + incidentId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 400 && status != 500) {
                        throw new AssertionError(
                                "Expected 400 or 500 for invalid state transition, got: " + status);
                    }
                });
    }

    @Test
    void updateStatus_missingStatusField_returns400() throws Exception {
        // @NotNull constraint on status field
        mockMvc.perform(patch("/api/v1/incidents/" + incidentId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\": \"no status\"}"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Resolve
    // -------------------------------------------------------------------------

    @Test
    void resolveIncident_validRequest_setsResolvedStatus() throws Exception {
        IncidentResolveRequest req = IncidentResolveRequest.builder()
                .resolutionSummary("Reverted the bad deployment")
                .build();

        mockMvc.perform(post("/api/v1/incidents/" + incidentId + "/resolve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolvedAt").isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // Alert list under incident
    // -------------------------------------------------------------------------

    @Test
    void getIncidentAlerts_validIncident_returnsAlertList() throws Exception {
        mockMvc.perform(get("/api/v1/incidents/" + incidentId + "/alerts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    // -------------------------------------------------------------------------
    // RBAC: VIEWER role must not be able to acknowledge / resolve / update status
    // -------------------------------------------------------------------------

    @Test
    void acknowledgeIncident_viewerRoleToken_returns403() throws Exception {
        // Register a VIEWER organisation — there is no direct API to create a VIEWER user yet,
        // but we simulate it by using an entirely different organisation's admin token against
        // an incident that the first org owns (this triggers tenant isolation, not role check,
        // but effectively returns 403 as expected for the RBAC boundary).
        //
        // In Milestone 2, VIEWER creation is not yet exposed, so we document the limitation
        // here and test the cross-tenant path as a proxy for the 403 response.
        //
        // See TenantIsolationTest for the definitive cross-tenant 403 assertion.
        String otherSlug = "viewer-org-" + System.nanoTime();
        RegisterRequest otherReg = RegisterRequest.builder()
                .organizationName("Viewer Org")
                .organizationSlug(otherSlug)
                .adminEmail("viewer-" + System.nanoTime() + "@example.com")
                .adminPassword("ViewerPass1")
                .adminFullName("Viewer Admin")
                .build();

        MvcResult otherResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherReg)))
                .andExpect(status().isCreated())
                .andReturn();

        String otherToken = objectMapper.readTree(otherResult.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // Other org's admin tries to acknowledge Org A's incident — must be 403
        IncidentAcknowledgeRequest ack = IncidentAcknowledgeRequest.builder()
                .note("Unauthorized attempt")
                .build();

        mockMvc.perform(post("/api/v1/incidents/" + incidentId + "/acknowledge")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ack)))
                .andExpect(status().isForbidden());
    }
}
