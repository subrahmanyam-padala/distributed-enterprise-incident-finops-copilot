package com.opspilot.controlplane.dto.finops;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostAnomalyResponse {
    private UUID id;
    private UUID organizationId;
    private String serviceName;
    private String environment;
    private LocalDate detectionDate;
    private BigDecimal actualAmount;
    private BigDecimal expectedAmount;
    private BigDecimal percentageChange;

    @JsonProperty("zScore")
    private BigDecimal zScore;

    private String severity;
    private UUID correlatedIncidentId;
    private String correlatedDeploymentVersion;
    private String status;
    private String explanation;
    private Instant detectedAt;
}
