package com.opspilot.controlplane.repository;

import com.opspilot.controlplane.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);
    Page<AuditLog> findByOrganizationIdAndEntityTypeOrderByCreatedAtDesc(UUID organizationId, String entityType, Pageable pageable);
}
