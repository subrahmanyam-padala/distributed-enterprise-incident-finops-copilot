package com.opspilot.controlplane.repository;

import com.opspilot.controlplane.domain.entity.CostAnomaly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CostAnomalyRepository extends JpaRepository<CostAnomaly, UUID> {

    List<CostAnomaly> findByOrganizationIdOrderByDetectionDateDesc(UUID organizationId);

    List<CostAnomaly> findByOrganizationIdAndServiceNameOrderByDetectionDateDesc(
            UUID organizationId, String serviceName);

    List<CostAnomaly> findByCorrelatedIncidentId(UUID incidentId);
}
