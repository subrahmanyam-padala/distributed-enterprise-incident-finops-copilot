package com.opspilot.controlplane.repository;

import com.opspilot.controlplane.domain.entity.Incident;
import com.opspilot.controlplane.domain.enums.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    Optional<Incident> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<Incident> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    Page<Incident> findByOrganizationIdAndStatusOrderByCreatedAtDesc(UUID organizationId, IncidentStatus status, Pageable pageable);

    List<Incident> findByOrganizationIdAndServiceNameAndStatusIn(UUID organizationId, String serviceName, List<IncidentStatus> statuses);

    Optional<Incident> findFirstByOrganizationIdAndServiceNameAndEnvironmentAndStatusInAndCreatedAtAfterOrderByCreatedAtDesc(
            UUID organizationId,
            String serviceName,
            String environment,
            List<IncidentStatus> statuses,
            Instant createdAfter
    );

    long countByOrganizationIdAndStatus(UUID organizationId, IncidentStatus status);
}
