package com.opspilot.controlplane.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "triage_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TriageReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "triage_id", nullable = false, unique = true)
    private UUID triageId;

    @Column(name = "confidence_score", precision = 4, scale = 3)
    private BigDecimal confidenceScore;

    @Column(name = "investigation_duration_ms")
    private Integer investigationDurationMs;

    @Column(columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private String observations = "[]";

    @Column(name = "supporting_evidence", columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private String supportingEvidence = "[]";

    @Column(name = "potential_root_causes", columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private String potentialRootCauses = "[]";

    @Column(name = "suggested_remediations", columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private String suggestedRemediations = "[]";

    @Column(name = "runbook_citations", columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private String runbookCitations = "[]";

    @Column(columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private String uncertainties = "[]";

    @Column(name = "additional_steps", columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private String additionalSteps = "[]";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
