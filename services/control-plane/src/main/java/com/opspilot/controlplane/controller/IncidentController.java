package com.opspilot.controlplane.controller;

import com.opspilot.controlplane.domain.enums.IncidentStatus;
import com.opspilot.controlplane.dto.alert.AlertResponse;
import com.opspilot.controlplane.dto.common.ApiResponse;
import com.opspilot.controlplane.dto.common.PagedResponse;
import com.opspilot.controlplane.dto.incident.IncidentAcknowledgeRequest;
import com.opspilot.controlplane.dto.incident.IncidentResolveRequest;
import com.opspilot.controlplane.dto.incident.IncidentResponse;
import com.opspilot.controlplane.dto.incident.IncidentStatusUpdateRequest;
import com.opspilot.controlplane.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents", description = "Tenant-isolated incident lifecycle management and state transitions")
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping
    @Operation(summary = "List incidents for the current tenant with optional status filter")
    public ResponseEntity<ApiResponse<PagedResponse<IncidentResponse>>> getIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        PagedResponse<IncidentResponse> response = incidentService.getIncidents(status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get incident details by ID (enforces strict tenant isolation)")
    public ResponseEntity<ApiResponse<IncidentResponse>> getIncidentById(@PathVariable UUID id) {
        IncidentResponse response = incidentService.getIncidentById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}/alerts")
    @Operation(summary = "Get list of alerts aggregated under an incident")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getIncidentAlerts(@PathVariable UUID id) {
        List<AlertResponse> alerts = incidentService.getAlertsForIncident(id);
        return ResponseEntity.ok(ApiResponse.ok(alerts));
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Acknowledge an incident (ADMIN or OPERATOR only)")
    public ResponseEntity<ApiResponse<IncidentResponse>> acknowledgeIncident(
            @PathVariable UUID id,
            @RequestBody(required = false) IncidentAcknowledgeRequest request
    ) {
        IncidentResponse response = incidentService.acknowledgeIncident(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Incident acknowledged", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Update incident status following valid lifecycle state transitions")
    public ResponseEntity<ApiResponse<IncidentResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody IncidentStatusUpdateRequest request
    ) {
        IncidentResponse response = incidentService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Incident status updated", response));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Resolve an incident with resolution summary")
    public ResponseEntity<ApiResponse<IncidentResponse>> resolveIncident(
            @PathVariable UUID id,
            @Valid @RequestBody IncidentResolveRequest request
    ) {
        IncidentResponse response = incidentService.resolveIncident(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Incident resolved successfully", response));
    }
}
