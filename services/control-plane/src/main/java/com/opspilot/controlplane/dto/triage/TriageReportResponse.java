package com.opspilot.controlplane.dto.triage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageReportResponse {

    private UUID id;
    private UUID incidentId;
    private UUID organizationId;
    private UUID triageId;
    private BigDecimal confidenceScore;
    private Integer investigationDurationMs;
    private String observations;
    private String supportingEvidence;
    private String potentialRootCauses;
    private String suggestedRemediations;
    private String runbookCitations;
    private String uncertainties;
    private String additionalSteps;
    private Instant createdAt;
}
