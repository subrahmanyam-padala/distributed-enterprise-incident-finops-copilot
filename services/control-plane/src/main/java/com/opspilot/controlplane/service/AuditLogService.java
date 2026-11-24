package com.opspilot.controlplane.service;

import com.opspilot.controlplane.domain.entity.AuditLog;
import com.opspilot.controlplane.dto.audit.AuditLogResponse;
import com.opspilot.controlplane.dto.common.PagedResponse;
import com.opspilot.controlplane.repository.AuditLogRepository;
import com.opspilot.controlplane.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void record(
            UUID organizationId,
            UUID userId,
            String action,
            String entityType,
            String entityId,
            String details,
            String ipAddress,
            String userAgent
    ) {
        AuditLog auditLog = AuditLog.builder()
                .organizationId(organizationId)
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details != null ? details : "{}")
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        auditLogRepository.save(auditLog);
        log.info("Audit: [org={}, user={}, action={}, entityType={}, entityId={}]",
                organizationId, userId, action, entityType, entityId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> getAuditLogs(Pageable pageable) {
        UUID organizationId = TenantContext.getOrganizationId();
        Page<AuditLog> page = auditLogRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId, pageable);

        return PagedResponse.<AuditLogResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private AuditLogResponse mapToResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .organizationId(a.getOrganizationId())
                .userId(a.getUserId())
                .action(a.getAction())
                .entityType(a.getEntityType())
                .entityId(a.getEntityId())
                .details(a.getDetails())
                .ipAddress(a.getIpAddress())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
