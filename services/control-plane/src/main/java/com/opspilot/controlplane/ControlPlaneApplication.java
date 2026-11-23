package com.opspilot.controlplane;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * OpsPilot Control Plane - Spring Boot Application Entry Point
 *
 * This service is the core Java control plane responsible for:
 *  - Multi-tenant authentication and authorisation (JWT + RBAC)
 *  - Alert ingestion via secured webhook endpoints
 *  - Incident lifecycle management and state transitions
 *  - Transactional outbox publishing to Apache Kafka
 *  - Kafka consumer for AI triage results
 *  - FinOps cost analytics APIs
 *  - Audit logging
 */
@SpringBootApplication
@EnableScheduling
public class ControlPlaneApplication {

    public static void main(String[] args) {
        SpringApplication.run(ControlPlaneApplication.class, args);
    }
}
