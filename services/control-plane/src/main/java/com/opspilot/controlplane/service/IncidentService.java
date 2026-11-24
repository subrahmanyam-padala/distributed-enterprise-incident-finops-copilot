package com.opspilot.controlplane.service;

import com.opspilot.controlplane.domain.entity.Alert;
import com.opspilot.controlplane.domain.entity.Incident;
import com.opspilot.controlplane.domain.enums.IncidentStatus;
import com.opspilot.controlplane.dto.alert.AlertResponse;
import com.opspilot.controlplane.dto.common.PagedResponse;
import com.opspilot.controlplane.dto.incident.IncidentAcknowledgeRequest;
import com.opspilot.controlplane.dto.incident.IncidentResolveRequest;
import com.opspilot.controlplane.dto.incident.IncidentResponse;
import com.opspilot.controlplane.dto.incident.IncidentStatusUpdateRequest;
import com.opspilot.controlplane.exception.ResourceNotFoundException;
import com.opspilot.controlplane.exception.TenantAccessDeniedException;
import com.opspilot.controlplane.repository.AlertRepository;
import com.opspilot.controlplane.repository.IncidentRepository;
import com.opspilot.controlplane.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final AlertRepository alertRepository;
    private final com.opspilot.controlplane.repository.TriageReportRepository triageReportRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public PagedResponse<IncidentResponse> getIncidents(IncidentStatus status, Pageable pageable) {
        UUID organizationId = requireCurrentOrgId();

        Page<Incident> page = (status != null)
                ? incidentRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(organizationId, status, pageable)
                : incidentRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId, pageable);

        return PagedResponse.<IncidentResponse>builder()
                .content(page.getContent().stream().map(this::mapToIncidentResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(UUID incidentId) {
        UUID organizationId = requireCurrentOrgId();

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with ID: " + incidentId));

        // Strict Tenant Isolation Verification
        if (!incident.getOrganizationId().equals(organizationId)) {
            log.warn("Blocked cross-tenant access: user org [{}] attempted to access incident [{}] of org [{}]",
                    organizationId, incidentId, incident.getOrganizationId());
            throw new TenantAccessDeniedException("Access denied: incident belongs to another organization");
        }

        return mapToIncidentResponse(incident);
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlertsForIncident(UUID incidentId) {
        // Enforce incident tenant access first
        getIncidentById(incidentId);

        return alertRepository.findByIncidentIdOrderByReceivedAtDesc(incidentId).stream()
                .map(this::mapToAlertResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<com.opspilot.controlplane.dto.triage.TriageReportResponse> getTriageReportsForIncident(UUID incidentId) {
        // Enforce incident tenant access first
        getIncidentById(incidentId);

        return triageReportRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId).stream()
                .map(tr -> com.opspilot.controlplane.dto.triage.TriageReportResponse.builder()
                        .id(tr.getId())
                        .incidentId(tr.getIncidentId())
                        .organizationId(tr.getOrganizationId())
                        .triageId(tr.getTriageId())
                        .confidenceScore(tr.getConfidenceScore())
                        .investigationDurationMs(tr.getInvestigationDurationMs())
                        .observations(tr.getObservations())
                        .supportingEvidence(tr.getSupportingEvidence())
                        .potentialRootCauses(tr.getPotentialRootCauses())
                        .suggestedRemediations(tr.getSuggestedRemediations())
                        .runbookCitations(tr.getRunbookCitations())
                        .uncertainties(tr.getUncertainties())
                        .additionalSteps(tr.getAdditionalSteps())
                        .createdAt(tr.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public IncidentResponse acknowledgeIncident(UUID incidentId, IncidentAcknowledgeRequest request) {
        UUID organizationId = requireCurrentOrgId();
        UUID userId = TenantContext.getUserId();

        Incident incident = findAndVerifyTenantAccess(incidentId, organizationId);

        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            throw new IllegalStateException("Cannot acknowledge a resolved incident");
        }

        incident.setAcknowledgedBy(userId);
        incident.setAcknowledgedAt(Instant.now());

        // Move to INVESTIGATING if currently OPEN
        if (incident.getStatus() == IncidentStatus.OPEN) {
            incident.setStatus(IncidentStatus.INVESTIGATING);
        }

        incident = incidentRepository.save(incident);

        auditLogService.record(
                organizationId,
                userId,
                "INCIDENT_ACKNOWLEDGED",
                "Incident",
                incident.getId().toString(),
                request != null && request.getNote() != null ? "{\"note\":\"" + request.getNote() + "\"}" : "{}",
                null,
                null
        );

        return mapToIncidentResponse(incident);
    }

    @Transactional
    public IncidentResponse updateStatus(UUID incidentId, IncidentStatusUpdateRequest request) {
        UUID organizationId = requireCurrentOrgId();
        UUID userId = TenantContext.getUserId();

        Incident incident = findAndVerifyTenantAccess(incidentId, organizationId);
        IncidentStatus currentStatus = incident.getStatus();
        IncidentStatus nextStatus = request.getStatus();

        validateStateTransition(currentStatus, nextStatus);

        incident.setStatus(nextStatus);
        if (nextStatus == IncidentStatus.RESOLVED) {
            incident.setResolvedAt(Instant.now());
        }

        incident = incidentRepository.save(incident);

        auditLogService.record(
                organizationId,
                userId,
                "INCIDENT_STATUS_UPDATED",
                "Incident",
                incident.getId().toString(),
                "{\"from\":\"" + currentStatus + "\",\"to\":\"" + nextStatus + "\"}",
                null,
                null
        );

        return mapToIncidentResponse(incident);
    }

    @Transactional
    public IncidentResponse resolveIncident(UUID incidentId, IncidentResolveRequest request) {
        UUID organizationId = requireCurrentOrgId();
        UUID userId = TenantContext.getUserId();

        Incident incident = findAndVerifyTenantAccess(incidentId, organizationId);

        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(Instant.now());

        if (request.getResolutionSummary() != null) {
            String currentSummary = incident.getSummary() != null ? incident.getSummary() + "\n\n" : "";
            incident.setSummary(currentSummary + "[Resolution] " + request.getResolutionSummary());
        }

        incident = incidentRepository.save(incident);

        auditLogService.record(
                organizationId,
                userId,
                "INCIDENT_RESOLVED",
                "Incident",
                incident.getId().toString(),
                "{\"resolutionSummary\":\"" + request.getResolutionSummary() + "\"}",
                null,
                null
        );

        return mapToIncidentResponse(incident);
    }

    private void validateStateTransition(IncidentStatus from, IncidentStatus to) {
        if (from == to) return;

        boolean valid = switch (from) {
            case OPEN -> to == IncidentStatus.INVESTIGATING || to == IncidentStatus.RESOLVED;
            case INVESTIGATING -> to == IncidentStatus.IDENTIFIED || to == IncidentStatus.MONITORING || to == IncidentStatus.RESOLVED;
            case IDENTIFIED -> to == IncidentStatus.MONITORING || to == IncidentStatus.RESOLVED || to == IncidentStatus.INVESTIGATING;
            case MONITORING -> to == IncidentStatus.RESOLVED || to == IncidentStatus.INVESTIGATING;
            case RESOLVED -> to == IncidentStatus.OPEN; // Reopen
        };

        if (!valid) {
            throw new IllegalStateException(
                    String.format("Invalid status transition from '%s' to '%s'", from, to)
            );
        }
    }

    private Incident findAndVerifyTenantAccess(UUID incidentId, UUID organizationId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with ID: " + incidentId));

        if (!incident.getOrganizationId().equals(organizationId)) {
            throw new TenantAccessDeniedException("Access denied: incident belongs to another organization");
        }
        return incident;
    }

    private UUID requireCurrentOrgId() {
        UUID orgId = TenantContext.getOrganizationId();
        if (orgId == null) {
            throw new TenantAccessDeniedException("Missing tenant context: unauthenticated access");
        }
        return orgId;
    }

    private IncidentResponse mapToIncidentResponse(Incident inc) {
        return IncidentResponse.builder()
                .id(inc.getId())
                .organizationId(inc.getOrganizationId())
                .title(inc.getTitle())
                .severity(inc.getSeverity())
                .status(inc.getStatus())
                .serviceName(inc.getServiceName())
                .environment(inc.getEnvironment())
                .alertCount(inc.getAlertCount())
                .summary(inc.getSummary())
                .resolvedAt(inc.getResolvedAt())
                .acknowledgedBy(inc.getAcknowledgedBy())
                .acknowledgedAt(inc.getAcknowledgedAt())
                .createdAt(inc.getCreatedAt())
                .updatedAt(inc.getUpdatedAt())
                .build();
    }

    private AlertResponse mapToAlertResponse(Alert a) {
        return AlertResponse.builder()
                .id(a.getId())
                .incidentId(a.getIncident() != null ? a.getIncident().getId() : null)
                .organizationId(a.getOrganizationId())
                .externalAlertId(a.getExternalAlertId())
                .source(a.getSource())
                .serviceName(a.getServiceName())
                .severity(a.getSeverity())
                .message(a.getMessage())
                .environment(a.getEnvironment())
                .fingerprint(a.getFingerprint())
                .receivedAt(a.getReceivedAt())
                .build();
    }
}
