package com.opspilot.controlplane.controller;

import com.opspilot.controlplane.dto.common.ApiResponse;
import com.opspilot.controlplane.dto.finops.CloudCostSummary;
import com.opspilot.controlplane.dto.finops.CostAnomalyResponse;
import com.opspilot.controlplane.security.TenantContext;
import com.opspilot.controlplane.service.FinOpsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finops")
@RequiredArgsConstructor
@Tag(name = "FinOps Intelligence", description = "Deterministic statistical cloud cost anomaly detection and financial attribution")
public class FinOpsController {

    private final FinOpsService finOpsService;

    @GetMapping("/anomalies")
    @Operation(summary = "Get list of detected cloud cost anomalies for current tenant")
    public ResponseEntity<ApiResponse<List<CostAnomalyResponse>>> getCostAnomalies() {
        List<CostAnomalyResponse> anomalies = finOpsService.getCostAnomalies();
        return ResponseEntity.ok(ApiResponse.ok(anomalies));
    }

    @GetMapping("/costs/daily")
    @Operation(summary = "Get daily cloud spend history across services")
    public ResponseEntity<ApiResponse<List<CloudCostSummary>>> getDailyCostHistory(
            @RequestParam(defaultValue = "30") int days
    ) {
        List<CloudCostSummary> history = finOpsService.getDailyCostHistory(days);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @PostMapping("/anomalies/detect")
    @Operation(summary = "Trigger deterministic statistical anomaly detection for a specific date")
    public ResponseEntity<ApiResponse<List<CostAnomalyResponse>>> triggerAnomalyDetection(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate target = (date != null) ? date : LocalDate.now();
        List<CostAnomalyResponse> detected = finOpsService.runAnomalyDetection(target);
        return ResponseEntity.ok(ApiResponse.ok("Anomaly detection executed", detected));
    }

    @PostMapping("/seed")
    @Operation(summary = "Seed sample cloud cost billing records for testing")
    public ResponseEntity<ApiResponse<String>> seedCostData() {
        UUID orgId = TenantContext.getOrganizationId();
        if (orgId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Tenant context required"));
        }
        finOpsService.seedSampleCostData(orgId);
        return ResponseEntity.ok(ApiResponse.ok("Sample FinOps billing data seeded successfully", "OK"));
    }
}
