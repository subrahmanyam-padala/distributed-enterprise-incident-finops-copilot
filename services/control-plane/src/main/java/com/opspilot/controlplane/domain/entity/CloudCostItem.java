package com.opspilot.controlplane.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cloud_cost_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloudCostItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 50)
    private String provider; // AWS, GCP, AZURE

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(nullable = false, length = 50)
    private String environment;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private String tags = "{}";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
