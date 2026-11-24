package com.opspilot.controlplane.repository;

import com.opspilot.controlplane.domain.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEvent.ProcessedEventId> {

    boolean existsByEventIdAndOrganizationId(String eventId, UUID organizationId);
}
