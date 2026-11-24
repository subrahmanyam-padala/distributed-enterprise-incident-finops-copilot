package com.opspilot.controlplane.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "external_alert_id")
    private String externalAlertId;

    @Column(nullable = false, length = 100)
    private String source;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 50)
    private String environment;

    @Column(nullable = false)
    private String fingerprint;

    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String labels = "{}";

    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String metadata = "{}";

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;
}
