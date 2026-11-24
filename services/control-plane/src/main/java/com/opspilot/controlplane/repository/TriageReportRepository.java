package com.opspilot.controlplane.repository;

import com.opspilot.controlplane.domain.entity.TriageReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TriageReportRepository extends JpaRepository<TriageReport, UUID> {

    Optional<TriageReport> findByTriageId(UUID triageId);

    List<TriageReport> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);

    List<TriageReport> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
