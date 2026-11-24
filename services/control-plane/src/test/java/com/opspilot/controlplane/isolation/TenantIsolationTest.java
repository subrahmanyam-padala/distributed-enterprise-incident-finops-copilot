package com.opspilot.controlplane.isolation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.controlplane.dto.alert.AlertWebhookRequest;
import com.opspilot.controlplane.dto.auth.RegisterRequest;
import com.opspilot.controlplane.dto.incident.IncidentAcknowledgeRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tenant Isolation Tests — the definitive portfolio demonstration that
 * the platform enforces strict multi-tenant data boundaries.
 *
 * <p>These tests prove that:
 * <ul>
 *   <li>Organisation B cannot read incidents belonging to Organisation A.</li>
 *   <li>Organisation B's incident list never leaks Organisation A's incidents.</li>
 *   <li>Organisation B cannot mutate (acknowledge/resolve) Organisation A's incidents.</li>
 *   <li>All cross-tenant attempts result in HTTP 403, never 404 (which could reveal existence).</li>
 * </ul>
 *
 * <p>This is a critical security correctness requirement for any SaaS platform.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Org A
    private String orgAToken;
    private UUID orgAId;
    private UUID orgAIncidentId;

    // Org B
    private String orgBToken;
    private UUID orgBId;

    @BeforeEach
    void setUp() throws Exception {
        // ---- Register Organisation A ----
        var orgAResult = registerOrg("org-a-" + System.nanoTime(), "admin-a");
        orgAToken = orgAResult.token();
        orgAId    = orgAResult.orgId();

        // ---- Ingest alert for Org A to create an incident ----
        AlertWebhookRequest alertA = AlertWebhookRequest.builder()
                .source("prometheus")
                .serviceName("user-service")
                .severity("HIGH")
                .message("DB connection pool exhausted")
                .environment("production")
                .organizationId(orgAId)
                .build();

        MvcResult alertResult = mockMvc.perform(post("/api/v1/alerts/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alertA)))
                .andExpect(status().isAccepted())
                .andReturn();

        orgAIncidentId = UUID.fromString(
                objectMapper.readTree(alertResult.getResponse().getContentAsString())
                        .path("data").path("id").asText()
        );

        // ---- Register Organisation B (completely separate tenant) ----
        var orgBResult = registerOrg("org-b-" + System.nanoTime(), "admin-b");
        orgBToken = orgBResult.token();
        orgBId    = orgBResult.orgId();
    }

    // -------------------------------------------------------------------------
    // Isolation: GET /incidents/{id}
    // -------------------------------------------------------------------------

    @Test
    void orgB_cannotReadOrgA_incident_byId_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/incidents/" + orgAIncidentId)
                        .header("Authorization", "Bearer " + orgBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    // -------------------------------------------------------------------------
    // Isolation: GET /incidents (list must not leak cross-tenant data)
    // -------------------------------------------------------------------------

    @Test
    void orgB_incidentList_doesNotContainOrgA_incidents() throws Exception {
        // Org B has no incidents yet — list must be empty
        MvcResult result = mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + orgBToken))
                .andExpect(status().isOk())
                .andReturn();

        var root = objectMapper.readTree(result.getResponse().getContentAsString());
        var content = root.path("data").path("content");

        // Verify none of the returned incidents belong to Org A
        for (var item : content) {
            String itemOrgId = item.path("organizationId").asText();
            assertThat(itemOrgId)
                    .as("Org B's incident list must never contain Org A incidents")
                    .isNotEqualTo(orgAId.toString());
        }

        // Verify Org A's specific incident is not present
        boolean containsOrgAIncident = false;
        for (var item : content) {
            if (orgAIncidentId.toString().equals(item.path("id").asText())) {
                containsOrgAIncident = true;
                break;
            }
        }
        assertThat(containsOrgAIncident)
                .as("Org B must not see Org A's incident in its list")
                .isFalse();
    }

    // -------------------------------------------------------------------------
    // Isolation: Mutation — acknowledge
    // -------------------------------------------------------------------------

    @Test
    void orgB_cannotAcknowledge_orgA_incident_returns403() throws Exception {
        IncidentAcknowledgeRequest ack = IncidentAcknowledgeRequest.builder()
                .note("Cross-tenant attack attempt")
                .build();

        mockMvc.perform(post("/api/v1/incidents/" + orgAIncidentId + "/acknowledge")
                        .header("Authorization", "Bearer " + orgBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ack)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Isolation: Mutation — resolve
    // -------------------------------------------------------------------------

    @Test
    void orgB_cannotResolve_orgA_incident_returns403() throws Exception {
        String resolveBody = """
                {"resolutionSummary": "Malicious resolve attempt by Org B"}
                """;

        mockMvc.perform(post("/api/v1/incidents/" + orgAIncidentId + "/resolve")
                        .header("Authorization", "Bearer " + orgBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolveBody))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Positive: Org A can still read its own incident normally
    // -------------------------------------------------------------------------

    @Test
    void orgA_canReadItsOwn_incident_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/incidents/" + orgAIncidentId)
                        .header("Authorization", "Bearer " + orgAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orgAIncidentId.toString()))
                .andExpect(jsonPath("$.data.organizationId").value(orgAId.toString()));
    }

    // -------------------------------------------------------------------------
    // Positive: Org B sees its own incidents when it has some
    // -------------------------------------------------------------------------

    @Test
    void orgB_canSeeItsOwn_incidents_afterIngestion() throws Exception {
        // Ingest an alert for Org B
        AlertWebhookRequest alertB = AlertWebhookRequest.builder()
                .source("datadog")
                .serviceName("reporting-service")
                .severity("MEDIUM")
                .message("Report generation timeout")
                .environment("staging")
                .organizationId(orgBId)
                .build();

        MvcResult alertResult = mockMvc.perform(post("/api/v1/alerts/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alertB)))
                .andExpect(status().isAccepted())
                .andReturn();

        String orgBIncidentId = objectMapper.readTree(alertResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Org B can read its own incident
        mockMvc.perform(get("/api/v1/incidents/" + orgBIncidentId)
                        .header("Authorization", "Bearer " + orgBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizationId").value(orgBId.toString()));

        // Org A cannot see Org B's incident
        mockMvc.perform(get("/api/v1/incidents/" + orgBIncidentId)
                        .header("Authorization", "Bearer " + orgAToken))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private record OrgCredentials(String token, UUID orgId) {}

    private OrgCredentials registerOrg(String slug, String adminName) throws Exception {
        RegisterRequest reg = RegisterRequest.builder()
                .organizationName("Isolation Test Org " + adminName)
                .organizationSlug(slug)
                .adminEmail(adminName + "-" + System.nanoTime() + "@isolation.example.com")
                .adminPassword("IsolationPass1")
                .adminFullName(adminName)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andReturn();

        var data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return new OrgCredentials(
                data.path("token").asText(),
                UUID.fromString(data.path("organization").path("id").asText())
        );
    }
}
