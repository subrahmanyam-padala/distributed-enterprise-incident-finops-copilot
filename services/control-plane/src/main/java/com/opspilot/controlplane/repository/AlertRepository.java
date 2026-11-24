package com.opspilot.controlplane.repository;

import com.opspilot.controlplane.domain.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Optional<Alert> findFirstByOrganizationIdAndFingerprintAndReceivedAtAfterOrderByReceivedAtDesc(
            UUID organizationId,
            String fingerprint,
            Instant receivedAfter
    );

    List<Alert> findByIncidentIdOrderByReceivedAtDesc(UUID incidentId);

    Page<Alert> findByOrganizationIdOrderByReceivedAtDesc(UUID organizationId, Pageable pageable);

    long countByIncidentId(UUID incidentId);
}
