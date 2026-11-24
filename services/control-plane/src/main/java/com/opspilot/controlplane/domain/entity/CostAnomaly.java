package com.opspilot.controlplane.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cost_anomalies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostAnomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(nullable = false, length = 50)
    private String environment;

    @Column(name = "detection_date", nullable = false)
    private LocalDate detectionDate;

    @Column(name = "actual_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal actualAmount;

    @Column(name = "expected_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal expectedAmount;

    @Column(name = "percentage_change", nullable = false, precision = 8, scale = 2)
    private BigDecimal percentageChange;

    @Column(name = "z_score", nullable = false, precision = 8, scale = 2)
    private BigDecimal zScore;

    @Column(nullable = false, length = 20)
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW

    @Column(name = "correlated_incident_id")
    private UUID correlatedIncidentId;

    @Column(name = "correlated_deployment_version", length = 100)
    private String correlatedDeploymentVersion;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "OPEN"; // OPEN, ACKNOWLEDGED, RESOLVED

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @CreationTimestamp
    @Column(name = "detected_at", nullable = false, updatable = false)
    private Instant detectedAt;
}
