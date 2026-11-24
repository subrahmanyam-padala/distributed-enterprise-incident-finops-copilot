package com.opspilot.controlplane.finops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opspilot.controlplane.dto.auth.RegisterRequest;
import com.opspilot.controlplane.dto.finops.CostAnomalyResponse;
import com.opspilot.controlplane.service.FinOpsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FinOpsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FinOpsService finOpsService;

    private String token;
    private UUID orgId;

    @BeforeEach
    void setUp() throws Exception {
        String slug = "finops-test-" + System.nanoTime();
        RegisterRequest reg = RegisterRequest.builder()
                .organizationName("FinOps Test Org")
                .organizationSlug(slug)
                .adminEmail("finops-" + System.nanoTime() + "@example.com")
                .adminPassword("FinOpsPass1")
                .adminFullName("FinOps Lead")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        var data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        token = data.path("token").asText();
        orgId = UUID.fromString(data.path("organization").path("id").asText());
    }

    @Test
    void finops_seedAndDetectStatisticalAnomaly_succeeds() throws Exception {
        // 1. Seed historical cost records
        mockMvc.perform(post("/api/v1/finops/seed")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 2. Query daily cost history
        mockMvc.perform(get("/api/v1/finops/costs/daily?days=14")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(10)));

        // 3. Trigger statistical anomaly detection on today's spiked spend
        MvcResult detectionResult = mockMvc.perform(post("/api/v1/finops/anomalies/detect")
                        .param("date", LocalDate.now().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        var detectNode = objectMapper.readTree(detectionResult.getResponse().getContentAsString());
        var anomalies = detectNode.path("data");
        assertThat(anomalies.isArray()).isTrue();
        assertThat(anomalies.size()).isGreaterThanOrEqualTo(1);

        var anomaly = anomalies.get(0);
        assertThat(anomaly.path("serviceName").asText()).isEqualTo("checkout-service");
        assertThat(anomaly.path("severity").asText()).isIn("CRITICAL", "HIGH");
        assertThat(anomaly.path("zScore").asDouble()).isGreaterThanOrEqualTo(2.0);
        assertThat(anomaly.path("percentageChange").asDouble()).isGreaterThan(100.0);

        // 4. List anomalies endpoint verifies persistence
        mockMvc.perform(get("/api/v1/finops/anomalies")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }
}
